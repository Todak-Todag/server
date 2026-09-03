package com.todak_todag.user_service.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserCommand.UserSignupCommand;
import com.todak_todag.user_service.user.application.command.UserCommand.UserSignupCommand.AgreementCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.result.UserResult.UserSignupCreatedResult;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;
import com.todak_todag.user_service.user.domain.repository.command.UserCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCreateService 단위테스트")
class UserCreateServiceTest {

	private static final UUID REGION_ID = UUID.fromString("3b9a8f7c-1d2e-4a5b-9c8d-7e6f5a4b3c2d");

	private static final UUID TERMS_ID = UUID.fromString("e012a1b2-c3d4-4e5f-8a9b-0c1d2e3f4a5b");

	private static final UUID SAVED_USER_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

	private static final String USERNAME = "example0123";

	private static final String RAW_PASSWORD = "Example0123@";

	private static final String HASHED_PASSWORD = "$2a$10$hashedvaluehashedvaluehashedvalue";

	private static final String NAME = "김영수";

	private static final String PHONE = "01012345678";

	@Mock
	private PasswordEncoderPort passwordEncoder;

	@Mock
	private UserCommandRepository userCommandRepo;

	@Mock
	private UserQueryRepository userQueryRepo;

	@InjectMocks
	private UserCreateService userCreateService;

	private static UserSignupCommand signupCommand(UserRole type) {
		return new UserSignupCommand(
				type,
				USERNAME,
				RAW_PASSWORD,
				NAME,
				PHONE,
				REGION_ID,
				List.of(new AgreementCommand(TERMS_ID, true))
		);
	}

	// 저장 시 DB 가 채워주는 PK 를 흉내낸다
	private static User withGeneratedId(User user) {
		ReflectionTestUtils.setField(user, "id", SAVED_USER_ID);
		return user;
	}

	@Test
	@DisplayName("유효한 회원가입 요청이면 User 를 저장하고 저장된 식별자와 이름을 반환한다")
	void createUserSignupTest_success() {
		// Given
		UserSignupCommand command = signupCommand(UserRole.HOSPITAL_STAFF);
		given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
		given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
		given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

		// When
		UserSignupCreatedResult result = userCreateService.createUserSignup(command);

		// Then
		verify(userCommandRepo, times(1)).save(any(User.class));
		assertThat(result.userId()).isEqualTo(SAVED_USER_ID);
		assertThat(result.name()).isEqualTo(NAME);
	}

	@Test
	@DisplayName("회원가입으로 생성된 User 의 상태는 PENDING 이다")
	void createUserSignupTest_statusIsPending() {
		// Given
		UserSignupCommand command = signupCommand(UserRole.SOCIAL_WORKER);
		given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
		given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
		given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

		// When
		userCreateService.createUserSignup(command);

		// Then
		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userCommandRepo).save(captor.capture());

		assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.PENDING);
	}

	@Test
	@DisplayName("비밀번호는 해시로 변환되어 저장되고 평문은 저장되지 않는다")
	void createUserSignupTest_passwordIsHashed() {
		// Given
		UserSignupCommand command = signupCommand(UserRole.SERVICE_PROVIDER);
		given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
		given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
		given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

		// When
		userCreateService.createUserSignup(command);

		// Then
		verify(passwordEncoder).encode(RAW_PASSWORD);

		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userCommandRepo).save(captor.capture());

		assertThat(captor.getValue().getPasswordHash())
				.isEqualTo(HASHED_PASSWORD)
				.isNotEqualTo(RAW_PASSWORD);
	}

	@Test
	@DisplayName("요청 값이 User 엔티티에 그대로 매핑된다")
	void createUserSignupTest_fieldMapping() {
		// Given
		UserSignupCommand command = signupCommand(UserRole.HOSPITAL_STAFF);
		given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
		given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
		given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

		// When
		userCreateService.createUserSignup(command);

		// Then
		ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
		verify(userCommandRepo).save(captor.capture());

		User saved = captor.getValue();

		assertThat(saved.getRegionId()).isEqualTo(REGION_ID);
		assertThat(saved.getUsername()).isEqualTo(USERNAME);
		assertThat(saved.getName()).isEqualTo(NAME);
		assertThat(saved.getPhone()).isEqualTo(PHONE);
		assertThat(saved.getRole()).isEqualTo(UserRole.HOSPITAL_STAFF);
	}

	@Test
	@DisplayName("로그인 아이디가 중복이면 USER_DUPLICATE_LOGIN_ID 예외가 발생하고 저장하지 않는다")
	void createUserSignupTest_fail_duplicateUsername() {
		// Given
		UserSignupCommand command = signupCommand(UserRole.HOSPITAL_STAFF);
		given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(true);

		// When & Then
		assertThatThrownBy(() -> userCreateService.createUserSignup(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(UserErrorCode.USER_DUPLICATE_LOGIN_ID);

		verify(userCommandRepo, never()).save(any(User.class));
	}

	@ParameterizedTest(name = "{0} 유형은 회원가입할 수 없다")
	@EnumSource(value = UserRole.class, names = {"PATIENT", "ADMIN", "MASTER"})
	@DisplayName("회원가입이 허용되지 않는 유형이면 USER_INVALID_CREATE_ROLE 예외가 발생하고 저장하지 않는다")
	void createUserSignupTest_fail_invalidRole(UserRole deniedRole) {
		// Given
		UserSignupCommand command = signupCommand(deniedRole);
		given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
		given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);

		// When & Then
		assertThatThrownBy(() -> userCreateService.createUserSignup(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(UserErrorCode.USER_INVALID_CREATE_ROLE);

		verify(userCommandRepo, never()).save(any(User.class));
	}

	@Test
	@DisplayName("로그인 아이디가 중복이면 비밀번호 해시 연산을 수행하지 않는다")
	void createUserSignupTest_fail_duplicateUsernameSkipsHashing() {
		// Given
		UserSignupCommand command = signupCommand(UserRole.HOSPITAL_STAFF);
		given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(true);

		// When
		assertThatThrownBy(() -> userCreateService.createUserSignup(command))
				.isInstanceOf(BusinessException.class);

		// Then
		verify(passwordEncoder, never()).encode(anyString());
	}

	@Test
	@DisplayName("정상 흐름은 중복 검증 - 비밀번호 해시 - 저장 순서로 수행된다")
	void createUserSignupTest_executionOrder() {
		// Given
		UserSignupCommand command = signupCommand(UserRole.HOSPITAL_STAFF);
		given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
		given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
		given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

		// When
		userCreateService.createUserSignup(command);

		// Then
		InOrder inOrder = inOrder(userQueryRepo, passwordEncoder, userCommandRepo);
		inOrder.verify(userQueryRepo).duplicateUsername(USERNAME);
		inOrder.verify(passwordEncoder).encode(RAW_PASSWORD);
		inOrder.verify(userCommandRepo).save(any(User.class));
	}
}
