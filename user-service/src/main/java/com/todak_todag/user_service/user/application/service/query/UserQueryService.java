package com.todak_todag.user_service.user.application.service.query;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.result.UserInternalReadResult;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

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
