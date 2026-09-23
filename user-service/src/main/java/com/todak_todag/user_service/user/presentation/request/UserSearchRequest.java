package com.todak_todag.user_service.user.presentation.request;

import java.util.Set;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.application.query.UserSearchQuery;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;

public record UserSearchRequest(
		Integer page,
		Integer size,
		Integer role,
		Integer status
) {

	public UserSearchQuery toQuery() {
		return new UserSearchQuery(
				page,
				size,
				resolveRoles(role),
				resolveStatus(status),
				null // regionId는 요청자 권한에 따라 서비스 계층에서 채워짐
		);
	}

	private static UserStatus resolveStatus(Integer code) {
		int resolved = (code == null) ? 1 : code;
		
		return switch (resolved) {
	    case 2 -> UserStatus.SUSPENDED;
	    
	    case 3 -> UserStatus.WITHDRAWN;
	    
	    case 4 -> UserStatus.PENDING;
	    
	    case 5 -> UserStatus.REJECTED;
	    
	    default -> UserStatus.APPROVED;
		};
	}

	private Set<UserRole> resolveRoles(Integer code) {
		int resolved = (code == null) ? 6 : code;
		
		return switch (resolved) {
	    case 1 -> Set.of(UserRole.PATIENT);
	    
	    case 2 -> Set.of(UserRole.HOSPITAL_STAFF);
	    
	    case 3 -> Set.of(UserRole.SERVICE_PROVIDER);
	    
	    case 4 -> Set.of(UserRole.SOCIAL_WORKER);
	    
	    case 5 -> Set.of(UserRole.ADMIN, UserRole.MASTER);
	    
	    default -> null; // null = 전체
		};
	}
	
	
}
