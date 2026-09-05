package com.todak_todag.user_service.user.application.service.query;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.result.UserInternalReadResult;
import com.todak_todag.user_service.user.application.service.result.UserInfoResult;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserQueryRepository userQueryRepository;
    
    private final UserQueryRepository userQueryRepo;
    
    private final RegionQueryRepository regionQueryRepo;

    public UserInternalReadResult getUser(UUID userId) {
        User user = userQueryRepo.findActiveById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        return new UserInternalReadResult(
                user.getId(),
                user.getRole(),
                user.getRegionId()
        );
    };
    
    public UserInfoResult getMe(UUID userId) {
    	// 1. 사용자를 먼저 조회한다.
    	User user = userQueryRepository.findActiveById(userId)
    			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    	
    	// 2. regionId 가 없을 수 있으니 미리 null 변수 준비
    	String province = null;
    	String district = null;
    	
    	boolean isAddressActive = false;
    	// 3. regionId 가 있을때만 조회한다.
    	if(user.isRegion()) {
    		Region region = regionQueryRepo.findById(user.getRegionId())
    				.orElseThrow(() -> new BusinessException(RegionErrorCode.REGION_NOT_FOUND));
    		
    		if(region.isActive()) {
    			isAddressActive = true;
    		}
    		
    		province = region.getProvince();
    		district = region.getDistrict();
    	}
    	
    	return new UserInfoResult(
    			user.getName(),
    			province,
    			district,
    			user.getPhone(),
    			user.getRegionId(),
    			user.getRole(),
    			isAddressActive
    	);
    }
    
    public Set<UUID> getMatchableSocialWorkers(UUID patientId) {
    	User patient = userQueryRepo.findById(patientId)
    			.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
    	
    	if(!patient.isPatient()) {
    		throw new BusinessException(UserErrorCode.USER_APPROVAL_CONFLICT);
    	}
    	
    	// 퇴원 예정자의 regionId 유무 검증
    	if(patient.getRegionId() == null || patient.getAddress() == null) {
    		throw new BusinessException(UserErrorCode.USER_PATIENT_INVALID_REGION);
    	}
    	
    	// 퇴원 예정자의 지역이 서비스 지원 지역인지 검증
    	if(!regionQueryRepo.existsAvailableRegion(patient.getRegionId())) {
    		throw new BusinessException(CommonErrorCode.REGION_NOT_SUPPORTED);
    	}
    	
    	return userQueryRepo.findMatchableSocialWorkerIds(patient.getRegionId());
    }
}
