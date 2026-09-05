package com.todak_todag.user_service.user.application.support;

import org.springframework.stereotype.Component;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserPatientCreateCommand;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AddressValidator {

	private final RegionQueryRepository regionQueryRepo;
	
	public void patientAddressValidate(UserPatientCreateCommand createPatient) {
		if (createPatient.regionId() == null) {
	    if (createPatient.address() != null && !createPatient.address().isBlank()) {
	    	// "지역 정보가 없을 때는 주소를 입력할 수 없습니다."
	    	throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_PATIENT_REGION);
	    }
	    return;
		}
		
		if (createPatient.address() == null || createPatient.address().isBlank()) {
			// "지역 정보가 지정된 경우 주소는 필수입니다."
			throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_PATINET_ADDRESS);
		}
		
		Region region = regionQueryRepo.findById(createPatient.regionId())
        .orElseThrow(() -> new BusinessException(RegionErrorCode.REGION_NOT_FOUND));
		
		boolean containsProvince = createPatient.address().contains(region.getProvince());
		boolean containsDistrict = createPatient.address().contains(region.getDistrict());
		
		if(!containsProvince || !containsDistrict) {
			// "주소에 선택한 지역 정보(시/도, 시/군/구)가 올바르게 포함되어 있지 않습니다."
			throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_PATIENT_REGION_ADDRESS_MISMATCH);
		}
	}
}
