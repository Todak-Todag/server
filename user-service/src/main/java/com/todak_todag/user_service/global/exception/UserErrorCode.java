package com.todak_todag.user_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
	
	
	;
	private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
