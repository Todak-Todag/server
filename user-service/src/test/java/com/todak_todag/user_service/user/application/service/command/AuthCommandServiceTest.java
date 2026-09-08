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
import com.todak_todag.user_service.global.exception.AuthErrorCode;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.AuthLoginCommand;
import com.todak_todag.user_service.user.application.command.AuthLogoutCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.application.port.TokenStorePort;
import com.todak_todag.user_service.user.application.result.AuthLoginResult;
import com.todak_todag.user_service.user.application.result.AuthReissueResult;
import com.todak_todag.user_service.user.application.support.TokenValidator;
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
	private TokenValidator tokenValidator;

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
				tokenValidator,
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

	@Nested
	@DisplayName("로그아웃")
	class Logout {

		private AuthLogoutCommand command(String accessToken) {
			UserContext user = UserContext.from(USER_ID.toString(), ROLE.name());
			return new AuthLogoutCommand(user, accessToken);
		}

		@Test
		@DisplayName("활성 세션과 accessToken이 모두 있으면 세션을 종료하고 AccessToken을 저장소에서 삭제한다")
		void logoutTest_activeSessionAndAccessToken_logsOutAndDeletesAccessToken() {
			// Given
			Auth activeSession = Auth.login(USER_ID, HASHED_REFRESH_TOKEN, LocalDateTime.now().plusDays(7), LocalDateTime.now());
			given(authQueryRepo.findActiveByUserId(USER_ID)).willReturn(Optional.of(activeSession));

			// When
			authCommandService.logout(command(ACCESS_TOKEN));

			// Then
			assertThat(activeSession.getLogoutAt()).isNotNull();
			verify(accessTokenStorePort).deleteAccessToken(ACCESS_TOKEN);
		}

		@Test
		@DisplayName("활성 세션이 없어도 accessToken이 있으면 AccessToken 삭제는 수행한다")
		void logoutTest_noActiveSession_stillDeletesAccessToken() {
			// Given
			given(authQueryRepo.findActiveByUserId(USER_ID)).willReturn(Optional.empty());

			// When
			authCommandService.logout(command(ACCESS_TOKEN));

			// Then
			verify(accessTokenStorePort).deleteAccessToken(ACCESS_TOKEN);
		}

		@Test
		@DisplayName("accessToken이 null이면 세션은 종료하되 AccessToken 저장소는 건드리지 않는다")
		void logoutTest_nullAccessToken_logsOutSessionOnlyWithoutTouchingTokenStore() {
			// Given
			Auth activeSession = Auth.login(USER_ID, HASHED_REFRESH_TOKEN, LocalDateTime.now().plusDays(7), LocalDateTime.now());
			given(authQueryRepo.findActiveByUserId(USER_ID)).willReturn(Optional.of(activeSession));

			// When
			authCommandService.logout(command(null));

			// Then
			assertThat(activeSession.getLogoutAt()).isNotNull();
			verifyNoInteractions(accessTokenStorePort);
		}

		@Test
		@DisplayName("accessToken이 빈 문자열이면 세션은 종료하되 AccessToken 저장소는 건드리지 않는다")
		void logoutTest_blankAccessToken_logsOutSessionOnlyWithoutTouchingTokenStore() {
			// Given
			Auth activeSession = Auth.login(USER_ID, HASHED_REFRESH_TOKEN, LocalDateTime.now().plusDays(7), LocalDateTime.now());
			given(authQueryRepo.findActiveByUserId(USER_ID)).willReturn(Optional.of(activeSession));

			// When
			authCommandService.logout(command(""));

			// Then
			assertThat(activeSession.getLogoutAt()).isNotNull();
			verifyNoInteractions(accessTokenStorePort);
		}
	}

	@Nested
	@DisplayName("토큰 재발급")
	class Reissue {

		private static final String NEW_ACCESS_TOKEN = "new-access-token-32chars-value";

		private static final String NEW_REFRESH_TOKEN = "new-refresh-token-32chars-value";

		private static final String NEW_JWT_ACCESS_TOKEN = "new-jwt-access-token";

		private static final String NEW_HASHED_REFRESH_TOKEN = "new-hashed-refresh-token";

		private Auth activeSession() {
			return Auth.login(USER_ID, HASHED_REFRESH_TOKEN, LocalDateTime.now().plusDays(7), LocalDateTime.now());
		}

		private Auth expiredSession() {
			return Auth.login(
					USER_ID,
					HASHED_REFRESH_TOKEN,
					LocalDateTime.now().minusMinutes(1),
					LocalDateTime.now().minusDays(8)
			);
		}

		// RefreshToken 검증 -> 세션 조회까지 정상 통과하는 상황을 세팅한다
		private void givenSessionResolvedFromRefreshToken(Auth session) {
			given(tokenPort.hashToken(REFRESH_TOKEN)).willReturn(HASHED_REFRESH_TOKEN);
			given(authQueryRepo.findActiveByRefreshTokenHash(HASHED_REFRESH_TOKEN)).willReturn(Optional.of(session));
		}

		// 세션 조회 -> 만료 검증 -> 소유자 조회 -> 새 토큰 발급까지 전부 정상 통과하는 상황을 세팅한다
		private void givenSuccessfulReissue(Auth session) {
			givenSessionResolvedFromRefreshToken(session);
			given(userQueryRepo.findActiveById(USER_ID)).willReturn(Optional.of(loginUser));
			given(loginUser.getId()).willReturn(USER_ID);
			given(loginUser.getRole()).willReturn(ROLE);
			given(tokenPort.createToken()).willReturn(NEW_ACCESS_TOKEN, NEW_REFRESH_TOKEN);
			given(tokenPort.createJwtAccessToken(USER_ID, ROLE)).willReturn(NEW_JWT_ACCESS_TOKEN);
			given(tokenPort.hashToken(NEW_REFRESH_TOKEN)).willReturn(NEW_HASHED_REFRESH_TOKEN);
		}

		@Test
		@DisplayName("성공하면 새 AccessToken/RefreshToken을 담은 결과를 반환한다")
		void reissueTest_success_returnsNewTokens() {
			// Given
			givenSuccessfulReissue(activeSession());

			// When
			AuthReissueResult result = authCommandService.reissue(REFRESH_TOKEN);

			// Then
			assertThat(result.newAccessToken()).isEqualTo(NEW_ACCESS_TOKEN);
			assertThat(result.newRefershToken()).isEqualTo(NEW_REFRESH_TOKEN);
		}

		@Test
		@DisplayName("성공하면 세션의 RefreshToken 해시가 새 값으로 회전된다")
		void reissueTest_success_rotatesSessionRefreshTokenHash() {
			// Given
			Auth session = activeSession();
			givenSuccessfulReissue(session);

			// When
			authCommandService.reissue(REFRESH_TOKEN);

			// Then
			assertThat(session.getRefreshTokenHash()).isEqualTo(NEW_HASHED_REFRESH_TOKEN);
		}

		@Test
		@DisplayName("성공하면 새 AccessToken을 저장소에 저장한다")
		void reissueTest_success_storesNewAccessToken() {
			// Given
			givenSuccessfulReissue(activeSession());

			// When
			authCommandService.reissue(REFRESH_TOKEN);

			// Then
			verify(accessTokenStorePort).storeAccessToken(NEW_ACCESS_TOKEN, NEW_JWT_ACCESS_TOKEN);
		}

		@Test
		@DisplayName("세션 소유자는 Auth 자신의 PK가 아니라 세션에 저장된 userId로 조회한다")
		void reissueTest_success_looksUpOwnerByAuthsUserId() {
			// Given
			Auth session = activeSession();
			givenSuccessfulReissue(session);

			// When
			authCommandService.reissue(REFRESH_TOKEN);

			// Then
			verify(userQueryRepo).findActiveById(session.getUserId());
		}

		@Test
		@DisplayName("리프레시 토큰 검증에 실패하면 그 예외가 그대로 전파되고 이후 단계는 수행되지 않는다")
		void reissueTest_invalidFormat_propagatesAndSkipsEverythingElse() {
			// Given
			willThrow(new BusinessException(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID))
					.given(tokenValidator).validateRefreshTokenCookie("bad-token");

			// When & Then
			assertThatThrownBy(() -> authCommandService.reissue("bad-token"))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);

			verifyNoInteractions(authQueryRepo, userQueryRepo, accessTokenStorePort);
		}

		@Test
		@DisplayName("해시로 조회되는 활성 세션이 없으면 AUTH_REFRESH_TOKEN_INVALID 예외가 발생하고 토큰을 발급하지 않는다")
		void reissueTest_sessionNotFound_throwsInvalidAndSkipsIssuance() {
			// Given
			given(tokenPort.hashToken(REFRESH_TOKEN)).willReturn(HASHED_REFRESH_TOKEN);
			given(authQueryRepo.findActiveByRefreshTokenHash(HASHED_REFRESH_TOKEN)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> authCommandService.reissue(REFRESH_TOKEN))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(AuthErrorCode.AUTH_REFRESH_TOKEN_INVALID);

			verifyNoInteractions(userQueryRepo, accessTokenStorePort);
		}

		@Test
		@DisplayName("세션은 찾았지만 만료됐으면 AUTH_REFRESH_TOKEN_EXPIRED 예외가 발생하고 토큰을 발급하지 않는다")
		void reissueTest_sessionExpired_throwsExpiredAndSkipsIssuance() {
			// Given
			givenSessionResolvedFromRefreshToken(expiredSession());

			// When & Then
			assertThatThrownBy(() -> authCommandService.reissue(REFRESH_TOKEN))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(AuthErrorCode.AUTH_REFRESH_TOKEN_EXPIRED);

			verifyNoInteractions(userQueryRepo, accessTokenStorePort);
		}

		@Test
		@DisplayName("세션 소유자를 활성 상태로 조회할 수 없으면 USER_NOT_FOUND 예외가 발생하고 토큰을 발급하지 않는다")
		void reissueTest_ownerNotActive_throwsUserNotFoundAndSkipsIssuance() {
			// Given
			givenSessionResolvedFromRefreshToken(activeSession());
			given(userQueryRepo.findActiveById(USER_ID)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> authCommandService.reissue(REFRESH_TOKEN))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_NOT_FOUND);

			verifyNoInteractions(accessTokenStorePort);
		}
	}
}
