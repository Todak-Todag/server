package com.todak_todag.user_service.user.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import com.todak_todag.user_service.user.application.command.UserAdminCreateCommand;
import com.todak_todag.user_service.user.application.command.UserSignupCommand;
import com.todak_todag.user_service.user.application.command.UserSignupCommand.AgreementCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.result.UserAdminCreatedResult;
import com.todak_todag.user_service.user.application.result.UserSignupCreatedResult;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;
import com.todak_todag.user_service.user.domain.repository.command.UserCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
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

	@Mock
	private RegionQueryRepository regionQueryRepo;

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

	private static UserAdminCreateCommand adminCreateCommand() {
		return new UserAdminCreateCommand(
				USERNAME,
				RAW_PASSWORD,
				NAME,
				PHONE,
				REGION_ID
		);
	}

	// 저장 시 DB 가 채워주는 PK 를 흉내낸다
	private static User withGeneratedId(User user) {
		ReflectionTestUtils.setField(user, "id", SAVED_USER_ID);
		return user;
	}

	@Nested
	@DisplayName("회원가입")
	class CreateUserSignup {

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

	/*
	 * TODO: Region 단건 조회(orElseThrow) 적용 후 아래 케이스를 추가한다
	 * 1. 응답의 province/district 는 요청 regionId 에 해당하는 지역 정보로 채워진다
	 * 2. 존재하지 않는 regionId 면 예외가 발생하고 저장하지 않는다
	 * 3. 지역 조회에 실패하면 중복 검증 - 비밀번호 해시 - 저장 이 수행되지 않는다
	 */
	@Nested
	@DisplayName("운영자 등록")
	class CreateUserAdmin {

		// TODO: Region 단건 조회로 교체되면 제거한다
		private void givenAvailableRegion() {
			given(regionQueryRepo.findAllAvailableRegions()).willReturn(List.of(mock(Region.class)));
		}

		@Test
		@DisplayName("유효한 운영자 등록 요청이면 User 를 저장하고 저장된 식별자와 이름을 반환한다")
		void createUserAdminTest_success() {
			// Given
			givenAvailableRegion();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			UserAdminCreatedResult result = userCreateService.createUserAdmin(adminCreateCommand());

			// Then
			verify(userCommandRepo, times(1)).save(any(User.class));
			assertThat(result.userId()).isEqualTo(SAVED_USER_ID);
			assertThat(result.name()).isEqualTo(NAME);
		}

		@Test
		@DisplayName("운영자로 생성된 User 의 상태는 APPROVED 이다")
		void createUserAdminTest_statusIsApproved() {
			// Given
			givenAvailableRegion();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserAdmin(adminCreateCommand());

			// Then
			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userCommandRepo).save(captor.capture());

			assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.APPROVED);
		}

		@Test
		@DisplayName("운영자로 생성된 User 의 권한은 ADMIN 이다")
		void createUserAdminTest_roleIsAdmin() {
			// Given
			givenAvailableRegion();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserAdmin(adminCreateCommand());

			// Then
			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userCommandRepo).save(captor.capture());

			assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
		}

		@Test
		@DisplayName("비밀번호는 해시로 변환되어 저장되고 평문은 저장되지 않는다")
		void createUserAdminTest_passwordIsHashed() {
			// Given
			givenAvailableRegion();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserAdmin(adminCreateCommand());

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
		void createUserAdminTest_fieldMapping() {
			// Given
			givenAvailableRegion();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserAdmin(adminCreateCommand());

			// Then
			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userCommandRepo).save(captor.capture());

			User saved = captor.getValue();

			assertThat(saved.getRegionId()).isEqualTo(REGION_ID);
			assertThat(saved.getUsername()).isEqualTo(USERNAME);
			assertThat(saved.getName()).isEqualTo(NAME);
			assertThat(saved.getPhone()).isEqualTo(PHONE);
		}

		@Test
		@DisplayName("로그인 아이디가 중복이면 USER_DUPLICATE_LOGIN_ID 예외가 발생하고 저장하지 않는다")
		void createUserAdminTest_fail_duplicateUsername() {
			// Given
			givenAvailableRegion();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(true);

			// When & Then
			assertThatThrownBy(() -> userCreateService.createUserAdmin(adminCreateCommand()))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_DUPLICATE_LOGIN_ID);

			verify(userCommandRepo, never()).save(any(User.class));
		}

		@Test
		@DisplayName("로그인 아이디가 중복이면 비밀번호 해시 연산을 수행하지 않는다")
		void createUserAdminTest_fail_duplicateUsernameSkipsHashing() {
			// Given
			givenAvailableRegion();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(true);

			// When
			assertThatThrownBy(() -> userCreateService.createUserAdmin(adminCreateCommand()))
					.isInstanceOf(BusinessException.class);

			// Then
			verify(passwordEncoder, never()).encode(anyString());
		}

		@Test
		@DisplayName("정상 흐름은 지역 조회 - 중복 검증 - 비밀번호 해시 - 저장 순서로 수행된다")
		void createUserAdminTest_executionOrder() {
			// Given
			givenAvailableRegion();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserAdmin(adminCreateCommand());

			// Then
			InOrder inOrder = inOrder(regionQueryRepo, userQueryRepo, passwordEncoder, userCommandRepo);
			inOrder.verify(regionQueryRepo).findAllAvailableRegions();
			inOrder.verify(userQueryRepo).duplicateUsername(USERNAME);
			inOrder.verify(passwordEncoder).encode(RAW_PASSWORD);
			inOrder.verify(userCommandRepo).save(any(User.class));
		}
	}
}
