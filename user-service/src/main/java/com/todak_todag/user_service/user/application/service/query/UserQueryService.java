package com.todak_todag.user_service.user.application.service.query;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.global.common.PageableFactory;
import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.RegionErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.port.UserSearchPort;
import com.todak_todag.user_service.user.application.query.UserSearchQuery;
import com.todak_todag.user_service.user.application.result.UserInternalReadResult;
import com.todak_todag.user_service.user.application.result.UserSearchResult;
import com.todak_todag.user_service.user.application.service.result.UserInfoResult;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserQueryRepository userQueryRepository;
    
    private final UserQueryRepository userQueryRepo;
    
    private final RegionQueryRepository regionQueryRepo;

    private final UserSearchPort userSearchPort;
    
    public UserInternalReadResult getUser(UUID userId) {
        User user = userQueryRepo.findActiveById(userId)
                .orElseThrow(() -> {
                    log.info("[User] 내부 조회 요청의 사용자를 찾을 수 없습니다. userId={}", userId);

                    return new BusinessException(UserErrorCode.USER_NOT_FOUND);
                });

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
    				.orElseThrow(() -> {
    					log.warn(
    							"[User] 사용자의 지역 정보가 존재하지 않는 지역을 가리킵니다. userId={}, regionId={}",
    							user.getId(),
    							user.getRegionId()
    					);

    					return new BusinessException(RegionErrorCode.REGION_NOT_FOUND);
    				});
    		
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
    			.orElseThrow(() -> {
    				log.info("[User] 매칭 대상 조회 요청의 퇴원 예정자를 찾을 수 없습니다. patientId={}", patientId);

    				return new BusinessException(UserErrorCode.USER_NOT_FOUND);
    			});

    	if(!patient.isPatient()) {
    		log.warn(
    				"[User] 퇴원 예정자가 아닌 사용자로 매칭 대상 조회가 요청되었습니다. userId={}, role={}",
    				patient.getId(),
    				patient.getRole()
    		);

    		throw new BusinessException(UserErrorCode.USER_APPROVAL_CONFLICT);
    	}

    	// 퇴원 예정자의 regionId 유무 검증
    	if(patient.getRegionId() == null || patient.getAddress() == null) {
    		log.info(
    				"[User] 지역·주소가 없는 퇴원 예정자라 매칭 대상을 조회할 수 없습니다. patientId={}",
    				patient.getId()
    		);

    		throw new BusinessException(UserErrorCode.USER_PATIENT_INVALID_REGION);
    	}

    	// 퇴원 예정자의 지역이 서비스 지원 지역인지 검증
    	if(!regionQueryRepo.existsAvailableRegion(patient.getRegionId())) {
    		log.info(
    				"[User] 서비스 지원 지역이 아니라 매칭 대상을 조회할 수 없습니다. patientId={}, regionId={}",
    				patient.getId(),
    				patient.getRegionId()
    		);

    		throw new BusinessException(CommonErrorCode.REGION_NOT_SUPPORTED);
    	}

    	Set<UUID> socialWorkerIds = userQueryRepo.findMatchableSocialWorkerIds(patient.getRegionId());

    	log.info(
    			"[User] 매칭 대상 사회복지사 조회 완료 patientId={}, regionId={}, matchableCount={}",
    			patient.getId(),
    			patient.getRegionId(),
    			socialWorkerIds.size()
    	);

    	return socialWorkerIds;
    }
    
    public Page<UserSearchResult> search(UserSearchQuery query, UserContext requester) {
    	UUID regionScope = resolveRegionScope(requester);

    	UserSearchQuery scopedQuery = new UserSearchQuery(
    			query.page(),
    			query.size(),
    			query.roles(),
    			query.status(),
    			regionScope
    	);

    	Pageable pageable = PageableFactory.of(scopedQuery.page(), scopedQuery.size(), null);

    	Page<UserSearchResult> result = userSearchPort.search(scopedQuery, pageable);

    	log.info(
    			"[User] 사용자 검색 조회 완료 requesterId={}, requesterRole={}, regionScope={}, resultCount={}",
    			requester.getUserId(),
    			requester.getRole(),
    			regionScope,
    			result.getNumberOfElements()
    	);

    	return result;
    }

    // ADMIN은 자신의 담당 지역내 사용자만 검색 가능 - MASTER는 제한 없음
    private UUID resolveRegionScope(UserContext requester) {
    	if(requester.getRole() != UserRole.ADMIN) {
    		return null;
    	}

    	User admin = userQueryRepo.findAdminById(requester.getUserId())
    			.orElseThrow(() -> {
    				log.warn(
    						"[User] ADMIN 권한 요청자의 운영자 정보를 찾을 수 없습니다. requesterId={}",
    						requester.getUserId()
    				);

    				return new BusinessException(CommonErrorCode.AUTH_FORBIDDEN);
    			});

    	return admin.getRegionId();
    }
}
