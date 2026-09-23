package com.todak_todag.user_service.user.application.support;

import org.springframework.stereotype.Component;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserPatientCreateCommand;
import com.todak_todag.user_service.user.application.command.UserUpdateCommand;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AddressValidator {

	private final RegionQueryRepository regionQueryRepo;
	
	public void updateAddressValidate(UserUpdateCommand userUpdate) {
		if(userUpdate.regionId() != null) {
			Region region = regionQueryRepo.findById(userUpdate.regionId())
	        .orElseThrow(() -> new BusinessException(RegionErrorCode.REGION_NOT_FOUND));
			
			if(!region.isActive()) {
				log.info(
						"[User] 서비스하지 않는 지역으로 회원정보 변경이 시도되었습니다. regionId={}",
						region.getId()
				);

				throw new BusinessException(CommonErrorCode.REGION_NOT_SUPPORTED);
			}

			if(userUpdate.address() != null && !userUpdate.address().isBlank()) {
				boolean containsProvince = userUpdate.address().contains(region.getProvince());
				boolean containsDistrict = userUpdate.address().contains(region.getDistrict());

				if(!containsProvince || !containsDistrict) {
					log.info(
							"[User] 지역과 대응하지 않는 상세 주소로 회원정보 변경이 시도되었습니다. regionId={}",
							userUpdate.regionId()
					);

					throw new BusinessException(UserErrorCode.USER_INVALID_REGION_ADDRESS_MISMATCH);
				}
			}

			return;
		}

		if(userUpdate.address() != null && !userUpdate.address().isBlank()) {
			log.info(
					"[User] 지역 정보 없이 상세 주소만으로 회원정보 변경이 시도되었습니다. userId={}",
					userUpdate.requesterId()
			);

			throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_PATIENT_REGION);
		}
	}
	
	public void patientAddressValidate(UserPatientCreateCommand createPatient) {
		if (createPatient.regionId() == null) {
	    if (createPatient.address() != null && !createPatient.address().isBlank()) {
	    	log.info(
						"[User] 지역 정보 없이 상세 주소만으로 퇴원 예정자 등록이 시도되었습니다. requesterId={}",
						createPatient.requesterId()
				);

	    	// "지역 정보가 없을 때는 주소를 입력할 수 없습니다."
	    	throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_PATIENT_REGION);
	    }
	    return;
		}

		if (createPatient.address() == null || createPatient.address().isBlank()) {
			log.info(
					"[User] 지역 정보는 있으나 상세 주소 없이 퇴원 예정자 등록이 시도되었습니다. requesterId={}",
					createPatient.requesterId()
			);

			// "지역 정보가 지정된 경우 주소는 필수입니다."
			throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_PATINET_ADDRESS);
		}
		
		Region region = regionQueryRepo.findById(createPatient.regionId())
        .orElseThrow(() -> new BusinessException(RegionErrorCode.REGION_NOT_FOUND));
		
		boolean containsProvince = createPatient.address().contains(region.getProvince());
		boolean containsDistrict = createPatient.address().contains(region.getDistrict());
		
		if(!containsProvince || !containsDistrict) {
			log.info(
					"[User] 지역과 대응하지 않는 상세 주소로 퇴원 예정자 등록이 시도되었습니다. requesterId={}, regionId={}",
					createPatient.requesterId(),
					createPatient.regionId()
			);

			// "주소에 선택한 지역 정보(시/도, 시/군/구)가 올바르게 포함되어 있지 않습니다."
			throw new BusinessException(UserErrorCode.USER_INVALID_REGION_ADDRESS_MISMATCH);
		}
	}
}
