package com.todak_todag.user_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
	
	USER_INVALID_USERNAME(HttpStatus.CONFLICT, "유효하지 않은 사용자 이름입니다."),
	
	USER_INVALID_CREATE_ROLE(HttpStatus.CONFLICT, "유저 등록을 진행할 수 없는 유형입니다."),
	
	USER_DUPLICATE_USERNAME(HttpStatus.CONFLICT, "이미 사용중인 로그인 아이디입니다."),
	;
	private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
