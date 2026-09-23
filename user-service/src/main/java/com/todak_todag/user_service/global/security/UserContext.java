package com.todak_todag.user_service.global.security;

import java.util.UUID;

import com.todak_todag.user_service.global.common.UserRole;

import lombok.Getter;

@Getter
public class UserContext {

	private final UUID userId;
	
	private final UserRole role;
	
	private UserContext(UUID userId, UserRole role) {
		this.userId = userId;
		this.role = role;
	}
	
	public static UserContext from(String userIdFromHeader, String roleFromHeader) {
		if(userIdFromHeader == null || roleFromHeader == null || userIdFromHeader.isBlank() || roleFromHeader.isBlank()) {
			return null;
		}
		
		try {
			UUID userId = UUID.fromString(userIdFromHeader);
			
			UserRole role = UserRole.valueOf(roleFromHeader);
			
			return new UserContext(userId, role);
			
		} catch (IllegalArgumentException e) {
			
			return null;
		}
	}
	
	public String getRoleName() {
		return this.role.name();
	}
	
}
