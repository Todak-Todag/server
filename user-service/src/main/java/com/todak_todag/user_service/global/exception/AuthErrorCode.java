package com.todak_todag.user_service.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	
	
	;
	private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}
