package com.todak_todag.api_gateway.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenErrorCode {

	UNAUTHORIZED("인증에 실패하였습니다.", HttpStatus.UNAUTHORIZED),
	
	INVALID_ACCESS_TOKEN("유효하지 않은 인증 정보입니다.", HttpStatus.UNAUTHORIZED),
	
	EXPIRED_ACCESS_TOKEN("액세스 토큰이 만료되었습니다", HttpStatus.UNAUTHORIZED),
	
	ACCESS_DENIED("접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
	
	AUTHENTICATION_SERVICE_UNAVAILABLE("인증 서비스를 일시적으로 사용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE)
	;
	private final String message;
	
	private final HttpStatus status;
	
	public String getCode() {
		return this.name();
	}
	
}
