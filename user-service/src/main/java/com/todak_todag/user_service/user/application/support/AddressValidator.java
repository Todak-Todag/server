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
				log.warn(
						"[User] AddressValidator. 활성 지역이 아닌 regionId 입니다. regionId={}",
						region.getId().toString()
				);
				
				throw new BusinessException(CommonErrorCode.REGION_NOT_SUPPORTED);
			}
			
			if(userUpdate.address() != null && !userUpdate.address().isBlank()) {
				boolean containsProvince = userUpdate.address().contains(region.getProvince());
				boolean containsDistrict = userUpdate.address().contains(region.getDistrict());
				
				if(!containsProvince || !containsDistrict) {
					log.warn(
							"[User] AddressValidator. 상세 주소가 regionId에 대응하지 않습니다. regionId={}",
							userUpdate.regionId().toString()
					);
					
					throw new BusinessException(UserErrorCode.USER_INVALID_REGION_ADDRESS_MISMATCH);
				}
			}
			
			return;
		}
		
		if(userUpdate.address() != null && !userUpdate.address().isBlank()) {
			log.warn(
					"[User] AddressValidator. 지역 정보가 없을 때 상세 주소를 입력할 수 없습니다. userId={}",
					userUpdate.requesterId().toString()
			);
			
			throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_PATIENT_REGION);
		}
	}
	
	public void patientAddressValidate(UserPatientCreateCommand createPatient) {
		if (createPatient.regionId() == null) {
	    if (createPatient.address() != null && !createPatient.address().isBlank()) {
	    	log.warn(
						"[User] AddressValidator. 지역 정보가 없을 때 상세 주소를 입력할 수 없습니다. requesterUserId={}",
						createPatient.requesterId().toString()
				);
	    	
	    	// "지역 정보가 없을 때는 주소를 입력할 수 없습니다."
	    	throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_PATIENT_REGION);
	    }
	    return;
		}
		
		if (createPatient.address() == null || createPatient.address().isBlank()) {
			log.warn(
					"[User] AddressValidator. 지역 정보가 지정된 퇴원 예정자 등록 요청 시 주소 정보가 필수입니다. requesterUserId={}",
					createPatient.requesterId().toString()
			);
			
			// "지역 정보가 지정된 경우 주소는 필수입니다."
			throw new BusinessException(UserErrorCode.USER_INVALID_CREATE_PATINET_ADDRESS);
		}
		
		Region region = regionQueryRepo.findById(createPatient.regionId())
        .orElseThrow(() -> new BusinessException(RegionErrorCode.REGION_NOT_FOUND));
		
		boolean containsProvince = createPatient.address().contains(region.getProvince());
		boolean containsDistrict = createPatient.address().contains(region.getDistrict());
		
		if(!containsProvince || !containsDistrict) {
			log.warn(
					"[User] AddressValidator. 퇴원 예정자 등록 요청의 상세 주소가 regionId에 대응하지 않습니다. userId={}, regionId={}",
					createPatient.requesterId().toString(),
					createPatient.regionId().toString()
			);
			
			// "주소에 선택한 지역 정보(시/도, 시/군/구)가 올바르게 포함되어 있지 않습니다."
			throw new BusinessException(UserErrorCode.USER_INVALID_REGION_ADDRESS_MISMATCH);
		}
	}
}
