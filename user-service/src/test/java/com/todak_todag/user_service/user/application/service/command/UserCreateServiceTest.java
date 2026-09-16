package com.todak_todag.user_service.user.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Set;
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
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserAdminCreateCommand;
import com.todak_todag.user_service.user.application.command.UserPatientCreateCommand;
import com.todak_todag.user_service.user.application.command.UserSignupCommand;
import com.todak_todag.user_service.user.application.command.UserSignupCommand.AgreementCommand;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.result.UserAdminCreatedResult;
import com.todak_todag.user_service.user.application.result.UserPatientCreatedResult;
import com.todak_todag.user_service.user.application.result.UserSignupCreatedResult;
import com.todak_todag.user_service.user.domain.entity.Consent;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;
import com.todak_todag.user_service.user.domain.repository.command.ConsentCommandRepository;
import com.todak_todag.user_service.user.domain.repository.command.UserCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

// 읽기 검증(지역/중복/약관/주소)은 UserCreateQueryService 로 분리되었다.
// 이 클래스는 Facade 가 BCrypt 를 끝낸 뒤 넘겨주는 passwordHash 를 받아
// "저장 직전 재검증 - 저장" 만 담당하므로, 그 범위만 검증한다.
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

	private static final String ADDRESS = "전라남도 고흥군 도양읍";

	private static final UUID HOSPITAL_STAFF_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440000");

	@Mock
	private UserCommandRepository userCommandRepo;

	@Mock
	private UserQueryRepository userQueryRepo;

	@Mock
	private ConsentCommandRepository consentCommandRepo;

	@Mock
	private PasswordEncoderPort passwordEncoder;

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

	private static UserPatientCreateCommand patientCreateCommand() {
		UserContext requester = UserContext.from(HOSPITAL_STAFF_ID.toString(), UserRole.HOSPITAL_STAFF.name());

		return new UserPatientCreateCommand(
				USERNAME,
				RAW_PASSWORD,
				NAME,
				PHONE,
				REGION_ID,
				ADDRESS,
				requester
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
		@DisplayName("유효한 요청이면 User 를 저장하고 저장된 식별자와 이름을 반환한다")
		void createUserSignupTest_success() {
			// Given
			UserSignupCommand command = signupCommand(UserRole.HOSPITAL_STAFF);
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			UserSignupCreatedResult result = userCreateService.createUserSignup(command, HASHED_PASSWORD, Set.of(TERMS_ID));

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
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserSignup(command, HASHED_PASSWORD, Set.of(TERMS_ID));

			// Then
			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userCommandRepo).save(captor.capture());

			assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.PENDING);
		}

		@Test
		@DisplayName("전달받은 passwordHash 가 그대로 저장되고 평문은 저장되지 않는다")
		void createUserSignupTest_storesGivenPasswordHash() {
			// Given
			UserSignupCommand command = signupCommand(UserRole.SERVICE_PROVIDER);
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserSignup(command, HASHED_PASSWORD, Set.of(TERMS_ID));

			// Then
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
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserSignup(command, HASHED_PASSWORD, Set.of(TERMS_ID));

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
		@DisplayName("저장 직전 재검증에서 로그인 아이디가 중복이면 USER_DUPLICATE_LOGIN_ID 예외가 발생하고 저장하지 않는다")
		void createUserSignupTest_fail_duplicateUsernameOnRecheck() {
			// Given
			UserSignupCommand command = signupCommand(UserRole.HOSPITAL_STAFF);
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(true);

			// When & Then
			assertThatThrownBy(() -> userCreateService.createUserSignup(command, HASHED_PASSWORD, Set.of(TERMS_ID)))
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

			// When & Then
			assertThatThrownBy(() -> userCreateService.createUserSignup(command, HASHED_PASSWORD, Set.of(TERMS_ID)))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_INVALID_CREATE_ROLE);

			verify(userCommandRepo, never()).save(any(User.class));
		}

		@Test
		@DisplayName("정상 흐름은 재검증 - 저장 - Consent 저장 순서로 수행된다")
		void createUserSignupTest_executionOrder() {
			// Given
			UserSignupCommand command = signupCommand(UserRole.HOSPITAL_STAFF);
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserSignup(command, HASHED_PASSWORD, Set.of(TERMS_ID));

			// Then
			InOrder inOrder = inOrder(userQueryRepo, userCommandRepo, consentCommandRepo);
			inOrder.verify(userQueryRepo).duplicateUsername(USERNAME);
			inOrder.verify(userCommandRepo).save(any(User.class));
			inOrder.verify(consentCommandRepo).saveAll(any());
		}

		@Test
		@DisplayName("전달받은 동의 약관 id 들이 저장된 유저 id로 저장된다")
		void createUserSignupTest_success_savesConsents() {
			// Given
			UserSignupCommand command = signupCommand(UserRole.HOSPITAL_STAFF);
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserSignup(command, HASHED_PASSWORD, Set.of(TERMS_ID));

			// Then
			ArgumentCaptor<List<Consent>> captor = ArgumentCaptor.forClass(List.class);
			verify(consentCommandRepo).saveAll(captor.capture());

			List<Consent> saved = captor.getValue();
			assertThat(saved).hasSize(1);
			assertThat(saved.get(0).getUserId()).isEqualTo(SAVED_USER_ID);
			assertThat(saved.get(0).getConsentDocumentVersionId()).isEqualTo(TERMS_ID);
		}

		@Test
		@DisplayName("동의한 약관이 없으면 Consent 저장은 빈 리스트로 호출된다")
		void createUserSignupTest_success_noAgreedConsents_savesEmptyList() {
			// Given
			UserSignupCommand command = signupCommand(UserRole.HOSPITAL_STAFF);
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserSignup(command, HASHED_PASSWORD, Set.of());

			// Then
			verify(consentCommandRepo).saveAll(List.of());
		}
	}

	@Nested
	@DisplayName("운영자 등록")
	class CreateUserAdmin {

		private Region region() {
			Region region = mock(Region.class);
			given(region.getProvince()).willReturn("전라남도");
			given(region.getDistrict()).willReturn("고흥군");
			return region;
		}

		@Test
		@DisplayName("유효한 요청이면 User 를 저장하고 저장된 식별자·이름·지역명을 반환한다")
		void createUserAdminTest_success() {
			// Given
			Region region = region();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			UserAdminCreatedResult result = userCreateService.createUserAdmin(adminCreateCommand(), HASHED_PASSWORD, region);

			// Then
			verify(userCommandRepo, times(1)).save(any(User.class));
			assertThat(result.userId()).isEqualTo(SAVED_USER_ID);
			assertThat(result.name()).isEqualTo(NAME);
			assertThat(result.province()).isEqualTo("전라남도");
			assertThat(result.district()).isEqualTo("고흥군");
		}

		@Test
		@DisplayName("운영자로 생성된 User 의 상태는 APPROVED 이고 권한은 ADMIN 이다")
		void createUserAdminTest_statusAndRole() {
			// Given
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserAdmin(adminCreateCommand(), HASHED_PASSWORD, region());

			// Then
			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userCommandRepo).save(captor.capture());

			assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.APPROVED);
			assertThat(captor.getValue().getRole()).isEqualTo(UserRole.ADMIN);
		}

		@Test
		@DisplayName("전달받은 passwordHash 가 그대로 저장되고 평문은 저장되지 않는다")
		void createUserAdminTest_storesGivenPasswordHash() {
			// Given
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserAdmin(adminCreateCommand(), HASHED_PASSWORD, region());

			// Then
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
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserAdmin(adminCreateCommand(), HASHED_PASSWORD, region());

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
		@DisplayName("저장 직전 재검증에서 로그인 아이디가 중복이면 USER_DUPLICATE_LOGIN_ID 예외가 발생하고 저장하지 않는다")
		void createUserAdminTest_fail_duplicateUsernameOnRecheck() {
			// Given
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(true);
			Region region = mock(Region.class);

			// When & Then
			assertThatThrownBy(() -> userCreateService.createUserAdmin(adminCreateCommand(), HASHED_PASSWORD, region))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_DUPLICATE_LOGIN_ID);

			verify(userCommandRepo, never()).save(any(User.class));
		}

		@Test
		@DisplayName("정상 흐름은 재검증 - 저장 순서로 수행된다")
		void createUserAdminTest_executionOrder() {
			// Given
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserAdmin(adminCreateCommand(), HASHED_PASSWORD, region());

			// Then
			InOrder inOrder = inOrder(userQueryRepo, userCommandRepo);
			inOrder.verify(userQueryRepo).duplicateUsername(USERNAME);
			inOrder.verify(userCommandRepo).save(any(User.class));
		}
	}

	@Nested
	@DisplayName("퇴원 예정자 등록")
	class CreateUserPatient {

		@Test
		@DisplayName("유효한 요청이면 User 를 저장하고 저장된 식별자와 요청자 식별자 등을 반환한다")
		void createUserPatientTest_success() {
			// Given
			UserPatientCreateCommand command = patientCreateCommand();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			UserPatientCreatedResult result = userCreateService.createUserPatient(command, HASHED_PASSWORD);

			// Then
			verify(userCommandRepo, times(1)).save(any(User.class));
			assertThat(result.patientId()).isEqualTo(SAVED_USER_ID);
			assertThat(result.hospitalStaffId()).isEqualTo(HOSPITAL_STAFF_ID);
			assertThat(result.name()).isEqualTo(NAME);
			assertThat(result.phone()).isEqualTo(PHONE);
			assertThat(result.regionId()).isEqualTo(REGION_ID);
		}

		@Test
		@DisplayName("퇴원 예정자로 생성된 User 의 상태는 WITHDRAWN 이고 권한은 PATIENT_CONSENT 이다")
		void createUserPatientTest_statusAndRole() {
			// Given
			UserPatientCreateCommand command = patientCreateCommand();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserPatient(command, HASHED_PASSWORD);

			// Then
			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userCommandRepo).save(captor.capture());

			assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.WITHDRAWN);
			// 약관 동의 전까지는 PATIENT 가 아니라 PATIENT_CONSENT 로 발급된다.
			// 이래야 다른 서비스의 PATIENT 전용 API(예: discharge-service 의 @PreAuthorize("hasAnyRole('HOSPITAL_STAFF','PATIENT')"))가
			// 인식하지 못하는 role 값이라 자동으로 거부하고, 임시 토큰의 권한 범위가 약관 동의로 한정된다.
			assertThat(captor.getValue().getRole()).isEqualTo(UserRole.PATIENT_CONSENT);
		}

		@Test
		@DisplayName("전달받은 passwordHash 가 그대로 저장되고 평문은 저장되지 않는다")
		void createUserPatientTest_storesGivenPasswordHash() {
			// Given
			UserPatientCreateCommand command = patientCreateCommand();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserPatient(command, HASHED_PASSWORD);

			// Then
			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userCommandRepo).save(captor.capture());

			assertThat(captor.getValue().getPasswordHash())
					.isEqualTo(HASHED_PASSWORD)
					.isNotEqualTo(RAW_PASSWORD);
		}

		@Test
		@DisplayName("요청 값이 User 엔티티에 그대로 매핑된다")
		void createUserPatientTest_fieldMapping() {
			// Given
			UserPatientCreateCommand command = patientCreateCommand();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserPatient(command, HASHED_PASSWORD);

			// Then
			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userCommandRepo).save(captor.capture());

			User saved = captor.getValue();

			assertThat(saved.getRegionId()).isEqualTo(REGION_ID);
			assertThat(saved.getUsername()).isEqualTo(USERNAME);
			assertThat(saved.getName()).isEqualTo(NAME);
			assertThat(saved.getPhone()).isEqualTo(PHONE);
			assertThat(saved.getAddress()).isEqualTo(ADDRESS);
		}

		@Test
		@DisplayName("저장 직전 재검증에서 로그인 아이디가 중복이면 USER_DUPLICATE_LOGIN_ID 예외가 발생하고 저장하지 않는다")
		void createUserPatientTest_fail_duplicateUsernameOnRecheck() {
			// Given
			UserPatientCreateCommand command = patientCreateCommand();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(true);

			// When & Then
			assertThatThrownBy(() -> userCreateService.createUserPatient(command, HASHED_PASSWORD))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_DUPLICATE_LOGIN_ID);

			verify(userCommandRepo, never()).save(any(User.class));
		}

		@Test
		@DisplayName("정상 흐름은 재검증 - 저장 순서로 수행된다")
		void createUserPatientTest_executionOrder() {
			// Given
			UserPatientCreateCommand command = patientCreateCommand();
			given(userQueryRepo.duplicateUsername(USERNAME)).willReturn(false);
			given(userCommandRepo.save(any(User.class))).willAnswer(i -> withGeneratedId(i.getArgument(0)));

			// When
			userCreateService.createUserPatient(command, HASHED_PASSWORD);

			// Then
			InOrder inOrder = inOrder(userQueryRepo, userCommandRepo);
			inOrder.verify(userQueryRepo).duplicateUsername(USERNAME);
			inOrder.verify(userCommandRepo).save(any(User.class));
		}
	}

	@Nested
	@DisplayName("마스터 계정 초기화")
	class CreateUserMaster {

		private static final UUID MASTER_ID = UUID.fromString("880e8400-e29b-41d4-a716-446655440000");

		@Test
		@DisplayName("설정된 ID의 계정이 없으면 MASTER/APPROVED 상태로, 설정된 ID 그대로 저장한다")
		void createUserMasterTest_success() {
			// Given
			given(userQueryRepo.initMasterDuplicate(MASTER_ID)).willReturn(false);
			given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);

			// When
			userCreateService.createUserMaster(MASTER_ID, USERNAME, RAW_PASSWORD, NAME, PHONE);

			// Then
			ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
			verify(userCommandRepo, times(1)).save(captor.capture());

			User saved = captor.getValue();
			assertThat(saved.getId()).isEqualTo(MASTER_ID);
			assertThat(saved.getUsername()).isEqualTo(USERNAME);
			assertThat(saved.getName()).isEqualTo(NAME);
			assertThat(saved.getPhone()).isEqualTo(PHONE);
			assertThat(saved.getRole()).isEqualTo(UserRole.MASTER);
			assertThat(saved.getStatus()).isEqualTo(UserStatus.APPROVED);
			assertThat(saved.getPasswordHash())
					.isEqualTo(HASHED_PASSWORD)
					.isNotEqualTo(RAW_PASSWORD);
		}

		@Test
		@DisplayName("설정된 ID의 계정이 이미 있으면 아무 것도 저장하지 않는다")
		void createUserMasterTest_alreadyExists_skips() {
			// Given
			given(userQueryRepo.initMasterDuplicate(MASTER_ID)).willReturn(true);

			// When
			userCreateService.createUserMaster(MASTER_ID, USERNAME, RAW_PASSWORD, NAME, PHONE);

			// Then
			verify(passwordEncoder, never()).encode(RAW_PASSWORD);
			verify(userCommandRepo, never()).save(any(User.class));
		}

		@Test
		@DisplayName("정상 흐름은 ID 기준 중복 검증 - 비밀번호 해시 - 저장 순서로 수행된다")
		void createUserMasterTest_executionOrder() {
			// Given
			given(userQueryRepo.initMasterDuplicate(MASTER_ID)).willReturn(false);
			given(passwordEncoder.encode(RAW_PASSWORD)).willReturn(HASHED_PASSWORD);

			// When
			userCreateService.createUserMaster(MASTER_ID, USERNAME, RAW_PASSWORD, NAME, PHONE);

			// Then
			InOrder inOrder = inOrder(userQueryRepo, passwordEncoder, userCommandRepo);
			inOrder.verify(userQueryRepo).initMasterDuplicate(MASTER_ID);
			inOrder.verify(passwordEncoder).encode(RAW_PASSWORD);
			inOrder.verify(userCommandRepo).save(any(User.class));
		}
	}
}
