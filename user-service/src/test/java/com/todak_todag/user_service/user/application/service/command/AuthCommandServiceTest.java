package com.todak_todag.user_service.user.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.AuthLoginCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.application.port.TokenStorePort;
import com.todak_todag.user_service.user.application.result.AuthResult.AuthLoginResult;
import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.AuthCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.AuthQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthCommandService 단위테스트")
class AuthCommandServiceTest {

	private static final UUID USER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

	private static final String USERNAME = "example0123";

	private static final String RAW_PASSWORD = "Example0123@";

	private static final String HASHED_PASSWORD = "$2a$10$hashedvaluehashedvaluehashedvalue";

	private static final String ACCESS_TOKEN = "access-token-32chars-random-value";

	private static final String REFRESH_TOKEN = "refresh-token-32chars-random-value";

	private static final String HASHED_REFRESH_TOKEN = "hashed-refresh-token";

	private static final String JWT_ACCESS_TOKEN = "jwt-access-token";

	private static final UserRole ROLE = UserRole.PATIENT;

	@Mock
	private TokenStorePort accessTokenStorePort;

	@Mock
	private TokenPort tokenPort;

	@Mock
	private PasswordEncoderPort passwordEncoder;

	@Mock
	private AuthCommandRepository authCommandRepo;

	@Mock
	private AuthQueryRepository authQueryRepo;

	@Mock
	private UserQueryRepository userQueryRepo;

	@Mock
	private User loginUser;

	private AuthCommandService authCommandService;

	@BeforeEach
	void setUp() {
		authCommandService = new AuthCommandService(
				Duration.ofDays(7),
				accessTokenStorePort,
				tokenPort,
				passwordEncoder,
				authCommandRepo,
				authQueryRepo,
				userQueryRepo
		);
	}

	private static AuthLoginCommand loginCommand() {
		return new AuthLoginCommand(USERNAME, RAW_PASSWORD);
	}

	// 아이디 조회 - 상태 검증 - 비밀번호 검증 - 토큰 발급까지 정상 통과하는 상황을 세팅한다
	private void givenSuccessfulAuthentication() {
		given(userQueryRepo.findLoginByUsername(USERNAME)).willReturn(Optional.of(loginUser));
		given(loginUser.getPasswordHash()).willReturn(HASHED_PASSWORD);
		given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(true);
		given(loginUser.getId()).willReturn(USER_ID);
		given(loginUser.getRole()).willReturn(ROLE);
		given(tokenPort.createToken()).willReturn(ACCESS_TOKEN, REFRESH_TOKEN);
		given(tokenPort.createJwtAccessToken(USER_ID, ROLE)).willReturn(JWT_ACCESS_TOKEN);
		given(tokenPort.hashToken(REFRESH_TOKEN)).willReturn(HASHED_REFRESH_TOKEN);
	}

	@Nested
	@DisplayName("정상 로그인")
	class Login_Success {

		@Test
		@DisplayName("성공하면 accessToken/refreshToken/userId를 담은 결과를 반환한다")
		void loginTest_success_returnsResult() {
			// Given
			givenSuccessfulAuthentication();
			given(authQueryRepo.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
			given(authCommandRepo.save(any(Auth.class))).willAnswer(i -> i.getArgument(0));

			// When
			AuthLoginResult result = authCommandService.login(loginCommand());

			// Then
			assertThat(result.userId()).isEqualTo(USER_ID);
			assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
			assertThat(result.refreshToken()).isEqualTo(REFRESH_TOKEN);
		}

		@Test
		@DisplayName("기존 활성 세션이 없으면 새 세션을 생성해서 저장한다")
		void loginTest_noActiveSession_createsNewSession() {
			// Given
			givenSuccessfulAuthentication();
			given(authQueryRepo.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
			given(authCommandRepo.save(any(Auth.class))).willAnswer(i -> i.getArgument(0));

			// When
			authCommandService.login(loginCommand());

			// Then
			ArgumentCaptor<Auth> captor = ArgumentCaptor.forClass(Auth.class);
			verify(authCommandRepo).save(captor.capture());

			assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
			assertThat(captor.getValue().getRefreshTokenHash()).isEqualTo(HASHED_REFRESH_TOKEN);
		}

		@Test
		@DisplayName("기존 활성 세션이 있으면 그 세션을 갱신하고 새로 저장하지 않는다")
		void loginTest_hasActiveSession_renewsExistingSession() {
			// Given
			givenSuccessfulAuthentication();

			LocalDateTime past = LocalDateTime.now().minusDays(1);
			Auth existingSession = Auth.login(USER_ID, "old-refresh-token-hash", past.plusDays(7), past);
			given(authQueryRepo.findActiveByUserId(USER_ID)).willReturn(Optional.of(existingSession));

			// When
			authCommandService.login(loginCommand());

			// Then
			assertThat(existingSession.getRefreshTokenHash()).isEqualTo(HASHED_REFRESH_TOKEN);
			verify(authCommandRepo, never()).save(any(Auth.class));
		}

		@Test
		@DisplayName("세션이 저장된 이후에 AccessToken을 저장소에 저장한다")
		void loginTest_storesAccessTokenAfterSessionPersisted() {
			// Given
			givenSuccessfulAuthentication();
			given(authQueryRepo.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
			given(authCommandRepo.save(any(Auth.class))).willAnswer(i -> i.getArgument(0));

			// When
			authCommandService.login(loginCommand());

			// Then
			InOrder inOrder = inOrder(authCommandRepo, accessTokenStorePort);
			inOrder.verify(authCommandRepo).save(any(Auth.class));
			inOrder.verify(accessTokenStorePort).storeAccessToken(ACCESS_TOKEN, JWT_ACCESS_TOKEN);
		}

		@Test
		@DisplayName("비밀번호는 요청의 원문 비밀번호와 사용자의 해시값으로 검증한다")
		void loginTest_passwordVerifiedWithCommandPasswordAndUserHash() {
			// Given
			givenSuccessfulAuthentication();
			given(authQueryRepo.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
			given(authCommandRepo.save(any(Auth.class))).willAnswer(i -> i.getArgument(0));

			// When
			authCommandService.login(loginCommand());

			// Then
			verify(passwordEncoder).matches(RAW_PASSWORD, HASHED_PASSWORD);
		}
	}

	@Nested
	@DisplayName("로그인 실패")
	class Login_Failure {

		@Test
		@DisplayName("존재하지 않는 사용자면 USER_LOGIN_MISMATCHED 예외가 발생하고 이후 단계는 수행되지 않는다")
		void loginTest_userNotFound_throwsAndSkipsEverythingElse() {
			// Given
			given(userQueryRepo.findLoginByUsername(USERNAME)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> authCommandService.login(loginCommand()))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_LOGIN_MISMATCHED);

			verifyNoInteractions(passwordEncoder, tokenPort, authCommandRepo, authQueryRepo, accessTokenStorePort);
		}

		@Test
		@DisplayName("로그인 불가 상태면 그 예외가 그대로 전파되고 비밀번호 검증 이후 단계는 수행되지 않는다")
		void loginTest_userCannotLogin_propagatesExceptionAndSkipsPasswordCheck() {
			// Given
			given(userQueryRepo.findLoginByUsername(USERNAME)).willReturn(Optional.of(loginUser));
			willThrow(new BusinessException(UserErrorCode.USER_SUSPENDED)).given(loginUser).validateCanLogin();

			// When & Then
			assertThatThrownBy(() -> authCommandService.login(loginCommand()))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_SUSPENDED);

			verifyNoInteractions(passwordEncoder, tokenPort, authCommandRepo, authQueryRepo, accessTokenStorePort);
		}

		@Test
		@DisplayName("비밀번호가 일치하지 않으면 USER_LOGIN_MISMATCHED 예외가 발생하고 토큰을 발급하지 않는다")
		void loginTest_passwordMismatch_throwsAndSkipsTokenIssuance() {
			// Given
			given(userQueryRepo.findLoginByUsername(USERNAME)).willReturn(Optional.of(loginUser));
			given(loginUser.getPasswordHash()).willReturn(HASHED_PASSWORD);
			given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(false);

			// When & Then
			assertThatThrownBy(() -> authCommandService.login(loginCommand()))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_LOGIN_MISMATCHED);

			verifyNoInteractions(tokenPort, authCommandRepo, authQueryRepo, accessTokenStorePort);
		}

		@Test
		@DisplayName("탈퇴 계정이고 비밀번호가 맞으면 USER_LOGIN_WITHDRAWN 예외가 발생하고 토큰을 발급하지 않는다")
		void loginTest_withdrawnUser_throwsAfterPasswordVerified() {
			// Given
			given(userQueryRepo.findLoginByUsername(USERNAME)).willReturn(Optional.of(loginUser));
			given(loginUser.getPasswordHash()).willReturn(HASHED_PASSWORD);
			given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(true);
			given(loginUser.isWithdrawn()).willReturn(true);

			// When & Then
			assertThatThrownBy(() -> authCommandService.login(loginCommand()))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_LOGIN_WITHDRAWN);

			verify(passwordEncoder).matches(RAW_PASSWORD, HASHED_PASSWORD);
			verifyNoInteractions(tokenPort, authCommandRepo, authQueryRepo, accessTokenStorePort);
		}

		@Test
		@DisplayName("탈퇴 계정이어도 비밀번호가 틀리면 WITHDRAWN이 아니라 USER_LOGIN_MISMATCHED로 응답하고 탈퇴 여부는 확인하지 않는다")
		void loginTest_withdrawnUser_wrongPassword_returnsMismatchedWithoutCheckingWithdrawn() {
			// Given
			given(userQueryRepo.findLoginByUsername(USERNAME)).willReturn(Optional.of(loginUser));
			given(loginUser.getPasswordHash()).willReturn(HASHED_PASSWORD);
			given(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).willReturn(false);

			// When & Then
			assertThatThrownBy(() -> authCommandService.login(loginCommand()))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_LOGIN_MISMATCHED);

			verify(loginUser, never()).isWithdrawn();
			verifyNoInteractions(tokenPort, authCommandRepo, authQueryRepo, accessTokenStorePort);
		}
	}

	@Nested
	@DisplayName("실행 순서")
	class Login_ExecutionOrder {

		@Test
		@DisplayName("사용자 조회 - 상태 검증 - 비밀번호 검증 - 세션 저장 - AccessToken 저장 순서로 수행된다")
		void loginTest_executionOrder() {
			// Given
			givenSuccessfulAuthentication();
			given(authQueryRepo.findActiveByUserId(USER_ID)).willReturn(Optional.empty());
			given(authCommandRepo.save(any(Auth.class))).willAnswer(i -> i.getArgument(0));

			// When
			authCommandService.login(loginCommand());

			// Then
			InOrder inOrder = inOrder(
					userQueryRepo,
					loginUser,
					passwordEncoder,
					authQueryRepo,
					authCommandRepo,
					accessTokenStorePort
			);
			inOrder.verify(userQueryRepo).findLoginByUsername(USERNAME);
			inOrder.verify(loginUser).validateCanLogin();
			inOrder.verify(passwordEncoder).matches(RAW_PASSWORD, HASHED_PASSWORD);
			inOrder.verify(authQueryRepo).findActiveByUserId(USER_ID);
			inOrder.verify(authCommandRepo).save(any(Auth.class));
			inOrder.verify(accessTokenStorePort).storeAccessToken(ACCESS_TOKEN, JWT_ACCESS_TOKEN);
		}
	}
}
