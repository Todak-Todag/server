package com.todak_todag.user_service.user.application.service.query;

import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.result.UserInternalReadResult;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserQueryRepository userQueryRepo;

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
    	
    	if(patient.getRegionId() == null || patient.getAddress() == null) {
    		throw new BusinessException(UserErrorCode.USER_MATCHABLE_REGION_NOT_FOUND);
    	}
    	
    	return userQueryRepo.findMatchableSocialWorkerIds(patient.getRegionId());
    }
}
