package com.todak_todag.user_service.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {
	
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 사용자입니다."),
	
	USER_INVALID_CREATE_ROLE(HttpStatus.CONFLICT, "유저 등록을 진행할 수 없는 유형입니다."),
	
	USER_DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용중인 로그인 아이디입니다."),
	
	USER_NOT_APPROVAL(HttpStatus.FORBIDDEN, "회원가입 승인 대기중인 계정입니다. 관리자 승인 후 로그인할 수 있습니다."),
	
	USER_SUSPENDED(HttpStatus.FORBIDDEN, "일시정지된 계정입니다."),
	
	USER_LOGIN_MISMATCHED(HttpStatus.CONFLICT, "아이디 또는 비밀번호가 일치하지 않습니다."),
	
	USER_LOGIN_WITHDRAWN(HttpStatus.FORBIDDEN, "동의 약관을 철회한 계정입니다. 서비스를 이용하시려면 다시 동의하여주세요."),
	
	USER_APPROVAL_CONFLICT(HttpStatus.CONFLICT, "승인할 사용자에게는 거절 사유를 입력할 수 없습니다."),
	
	USER_REJECT_CONFLICT(HttpStatus.CONFLICT, "거절할 사용자에게는 거절 사유를 입력해야 합니다."),
	
	USER_MODIFY_STATE(HttpStatus.CONFLICT, "대상 사용자는 승인/거절이 불가능한 상태입니다."),
	
	USER_INVALID_CREATE_PATIENT_REGION(HttpStatus.CONFLICT, "지역 정보가 없을 때는 주소를 입력할 수 없습니다."),
	
	USER_INVALID_CREATE_PATINET_ADDRESS(HttpStatus.CONFLICT, "지역 정보가 지정된 경우 주소는 필수입니다."),
	
	USER_INVALID_CREATE_PATIENT_REGION_ADDRESS_MISMATCH(HttpStatus.CONFLICT, "주소에 선택한 지역 정보(시/도, 시/군/구)가 올바르게 포함되어 있지 않습니다."), 
	
	USER_SUSPEND_MODIFY_STATE(HttpStatus.CONFLICT, "대상 사용자를 정지시킬 수 있는 상태가 아닙니다."),
	
	
	;
	private final HttpStatus status;
  private final String message;

  @Override
  public String getCode() {
    return name();
  }
}