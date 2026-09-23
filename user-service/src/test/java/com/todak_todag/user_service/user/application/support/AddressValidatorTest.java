package com.todak_todag.user_service.user.application.support;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserPatientCreateCommand;
import com.todak_todag.user_service.user.application.command.UserUpdateCommand;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressValidator 단위테스트")
class AddressValidatorTest {

	private static final UUID REGION_ID = UUID.fromString("3b9a8f7c-1d2e-4a5b-9c8d-7e6f5a4b3c2d");

	private static final String USERNAME = "example0123";

	private static final String RAW_PASSWORD = "Example0123@";

	private static final String NAME = "김영수";

	private static final String PHONE = "01012345678";

	private static final UUID HOSPITAL_STAFF_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440000");

	@Mock
	private RegionQueryRepository regionQueryRepo;

	@InjectMocks
	private AddressValidator addressValidator;

	private static UserPatientCreateCommand patientCommand(UUID regionId, String address) {
		UserContext requester = UserContext.from(HOSPITAL_STAFF_ID.toString(), "HOSPITAL_STAFF");

		return new UserPatientCreateCommand(
				USERNAME,
				RAW_PASSWORD,
				NAME,
				PHONE,
				regionId,
				address,
				requester
		);
	}

	private static UserUpdateCommand updateCommand(UUID regionId, String address) {
		UserContext requester = UserContext.from(HOSPITAL_STAFF_ID.toString(), "HOSPITAL_STAFF");

		return new UserUpdateCommand(null, null, regionId, address, requester);
	}

	@Nested
	@DisplayName("퇴원 예정자 주소 검증")
	class PatientAddressValidate {

		@Test
		@DisplayName("지역 정보가 없고 주소도 없으면 예외 없이 통과한다")
		void patientAddressValidateTest_noRegionNoAddress_success() {
			// Given
			UserPatientCreateCommand command = patientCommand(null, null);

			// When & Then
			assertThatCode(() -> addressValidator.patientAddressValidate(command))
					.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("지역 정보가 없는데 주소가 입력되면 USER_INVALID_CREATE_PATIENT_REGION 예외가 발생한다")
		void patientAddressValidateTest_noRegionWithAddress_fail() {
			// Given
			UserPatientCreateCommand command = patientCommand(null, "전라남도 고흥군 도양읍");

			// When & Then
			assertThatThrownBy(() -> addressValidator.patientAddressValidate(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_INVALID_CREATE_PATIENT_REGION);
		}

		@Test
		@DisplayName("지역 정보가 있는데 주소가 없으면 USER_INVALID_CREATE_PATINET_ADDRESS 예외가 발생한다")
		void patientAddressValidateTest_regionWithNoAddress_fail() {
			// Given
			UserPatientCreateCommand command = patientCommand(REGION_ID, "  ");

			// When & Then
			assertThatThrownBy(() -> addressValidator.patientAddressValidate(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_INVALID_CREATE_PATINET_ADDRESS);
		}

		@Test
		@DisplayName("지역 정보에 해당하는 지역이 존재하지 않으면 REGION_NOT_FOUND 예외가 발생한다")
		void patientAddressValidateTest_regionNotFound_fail() {
			// Given
			UserPatientCreateCommand command = patientCommand(REGION_ID, "전라남도 고흥군 도양읍");

			given(regionQueryRepo.findById(REGION_ID)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> addressValidator.patientAddressValidate(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(RegionErrorCode.REGION_NOT_FOUND);
		}

		@Test
		@DisplayName("주소에 지역의 시/도, 시/군/구가 모두 포함되면 예외 없이 통과한다")
		void patientAddressValidateTest_addressMatchesRegion_success() {
			// Given
			UserPatientCreateCommand command = patientCommand(REGION_ID, "전라남도 고흥군 도양읍");

			Region region = Mockito.mock(Region.class);
			given(region.getProvince()).willReturn("전라남도");
			given(region.getDistrict()).willReturn("고흥군");
			given(regionQueryRepo.findById(REGION_ID)).willReturn(Optional.of(region));

			// When & Then
			assertThatCode(() -> addressValidator.patientAddressValidate(command))
					.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("주소에 시/도 정보가 빠져 있으면 USER_INVALID_CREATE_PATIENT_REGION_ADDRESS_MISMATCH 예외가 발생한다")
		void patientAddressValidateTest_addressMissingProvince_fail() {
			// Given
			UserPatientCreateCommand command = patientCommand(REGION_ID, "고흥군 도양읍");

			Region region = Mockito.mock(Region.class);
			given(region.getProvince()).willReturn("전라남도");
			given(region.getDistrict()).willReturn("고흥군");
			given(regionQueryRepo.findById(REGION_ID)).willReturn(Optional.of(region));

			// When & Then
			assertThatThrownBy(() -> addressValidator.patientAddressValidate(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_INVALID_REGION_ADDRESS_MISMATCH);
		}

		@Test
		@DisplayName("주소에 시/군/구 정보가 빠져 있으면 USER_INVALID_CREATE_PATIENT_REGION_ADDRESS_MISMATCH 예외가 발생한다")
		void patientAddressValidateTest_addressMissingDistrict_fail() {
			// Given
			UserPatientCreateCommand command = patientCommand(REGION_ID, "전라남도 도양읍");

			Region region = Mockito.mock(Region.class);
			given(region.getProvince()).willReturn("전라남도");
			given(region.getDistrict()).willReturn("고흥군");
			given(regionQueryRepo.findById(REGION_ID)).willReturn(Optional.of(region));

			// When & Then
			assertThatThrownBy(() -> addressValidator.patientAddressValidate(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_INVALID_REGION_ADDRESS_MISMATCH);
		}
	}

	@Nested
	@DisplayName("내 정보 수정 주소 검증")
	class UpdateAddressValidate {

		@Test
		@DisplayName("regionId도 주소도 없으면 지역 조회 없이 예외 없이 통과한다")
		void updateAddressValidateTest_noRegionNoAddress_success() {
			// Given
			UserUpdateCommand command = updateCommand(null, null);

			// When & Then
			assertThatCode(() -> addressValidator.updateAddressValidate(command))
					.doesNotThrowAnyException();

			verify(regionQueryRepo, never()).findById(any());
		}

		@Test
		@DisplayName("regionId 없이 주소만 보내면 USER_INVALID_CREATE_PATIENT_REGION 예외가 발생한다")
		void updateAddressValidateTest_noRegionWithAddress_fail() {
			// Given
			UserUpdateCommand command = updateCommand(null, "전라남도 고흥군 도양읍");

			// When & Then
			assertThatThrownBy(() -> addressValidator.updateAddressValidate(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_INVALID_CREATE_PATIENT_REGION);

			verify(regionQueryRepo, never()).findById(any());
		}

		@Test
		@DisplayName("regionId는 있는데 주소가 없으면 지역만 조회하고 주소 일치 검증 없이 통과한다")
		void updateAddressValidateTest_regionWithNoAddress_success() {
			// Given
			UserUpdateCommand command = updateCommand(REGION_ID, null);

			Region region = Mockito.mock(Region.class);
			given(region.isActive()).willReturn(true);
			given(regionQueryRepo.findById(REGION_ID)).willReturn(Optional.of(region));

			// When & Then
			assertThatCode(() -> addressValidator.updateAddressValidate(command))
					.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("regionId에 해당하는 지역이 서비스 지원 중이 아니면 REGION_NOT_SUPPORTED 예외가 발생한다")
		void updateAddressValidateTest_regionNotActive_fail() {
			// Given
			UserUpdateCommand command = updateCommand(REGION_ID, "전라남도 고흥군 도양읍");

			Region region = Mockito.mock(Region.class);
			given(region.isActive()).willReturn(false);
			given(regionQueryRepo.findById(REGION_ID)).willReturn(Optional.of(region));

			// When & Then
			assertThatThrownBy(() -> addressValidator.updateAddressValidate(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(CommonErrorCode.REGION_NOT_SUPPORTED);
		}

		@Test
		@DisplayName("regionId에 해당하는 지역이 존재하지 않으면 REGION_NOT_FOUND 예외가 발생한다")
		void updateAddressValidateTest_regionNotFound_fail() {
			// Given
			UserUpdateCommand command = updateCommand(REGION_ID, "전라남도 고흥군 도양읍");

			given(regionQueryRepo.findById(REGION_ID)).willReturn(Optional.empty());

			// When & Then
			assertThatThrownBy(() -> addressValidator.updateAddressValidate(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(RegionErrorCode.REGION_NOT_FOUND);
		}

		@Test
		@DisplayName("주소에 지역의 시/도, 시/군/구가 모두 포함되면 예외 없이 통과한다")
		void updateAddressValidateTest_addressMatchesRegion_success() {
			// Given
			UserUpdateCommand command = updateCommand(REGION_ID, "전라남도 고흥군 도양읍");

			Region region = Mockito.mock(Region.class);
			given(region.isActive()).willReturn(true);
			given(region.getProvince()).willReturn("전라남도");
			given(region.getDistrict()).willReturn("고흥군");
			given(regionQueryRepo.findById(REGION_ID)).willReturn(Optional.of(region));

			// When & Then
			assertThatCode(() -> addressValidator.updateAddressValidate(command))
					.doesNotThrowAnyException();
		}

		@Test
		@DisplayName("주소에 시/도 정보가 빠져 있으면 USER_INVALID_REGION_ADDRESS_MISMATCH 예외가 발생한다")
		void updateAddressValidateTest_addressMissingProvince_fail() {
			// Given
			UserUpdateCommand command = updateCommand(REGION_ID, "고흥군 도양읍");

			Region region = Mockito.mock(Region.class);
			given(region.isActive()).willReturn(true);
			given(region.getProvince()).willReturn("전라남도");
			given(region.getDistrict()).willReturn("고흥군");
			given(regionQueryRepo.findById(REGION_ID)).willReturn(Optional.of(region));

			// When & Then
			assertThatThrownBy(() -> addressValidator.updateAddressValidate(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_INVALID_REGION_ADDRESS_MISMATCH);
		}

		@Test
		@DisplayName("주소에 시/군/구 정보가 빠져 있으면 USER_INVALID_REGION_ADDRESS_MISMATCH 예외가 발생한다")
		void updateAddressValidateTest_addressMissingDistrict_fail() {
			// Given
			UserUpdateCommand command = updateCommand(REGION_ID, "전라남도 도양읍");

			Region region = Mockito.mock(Region.class);
			given(region.isActive()).willReturn(true);
			given(region.getProvince()).willReturn("전라남도");
			given(region.getDistrict()).willReturn("고흥군");
			given(regionQueryRepo.findById(REGION_ID)).willReturn(Optional.of(region));

			// When & Then
			assertThatThrownBy(() -> addressValidator.updateAddressValidate(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_INVALID_REGION_ADDRESS_MISMATCH);
		}
	}
}
