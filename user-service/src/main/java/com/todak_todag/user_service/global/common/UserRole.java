package com.todak_todag.user_service.global.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {
	PATIENT("퇴원 예정자"),
	
	HOSPITAL_STAFF("병원 담당자"),
	
	SERVICE_PROVIDER("서비스 제공자"),
	
	SOCIAL_WORKER("사회복지사"),
	
	ADMIN("운영자"),
	
	MASTER("관리자"),
	
	;
	
	private final String KoreaName;
}
