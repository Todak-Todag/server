package com.todak_todag.api_gateway.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TokenErrorCode {

	UNAUTHORIZED("인증에 실패하였습니다.", HttpStatus.UNAUTHORIZED),
	
	INVALID_ACCESS_TOKEN("유효하지 않은 인증 정보입니다.", HttpStatus.UNAUTHORIZED)
	;
	private final String message;
	
	private final HttpStatus status;
	
	public String getCode() {
		return this.name();
	}
	
}
