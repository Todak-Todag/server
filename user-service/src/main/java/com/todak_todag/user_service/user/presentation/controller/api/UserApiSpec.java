package com.todak_todag.user_service.user.presentation.controller.api;

import org.springframework.http.ResponseEntity;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.presentation.request.UserAdminCreateRequest;
import com.todak_todag.user_service.user.presentation.request.UserApprovalRequest;
import com.todak_todag.user_service.user.presentation.request.UserPatientCreateRequest;
import com.todak_todag.user_service.user.presentation.request.UserSignupRequest;
import com.todak_todag.user_service.user.presentation.response.UserAdminCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserApprovalResponse;
import com.todak_todag.user_service.user.presentation.response.UserPatientCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserSignupCreatedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Service User", description = "사용자 API")
public interface UserApiSpec {

	@Operation(
			summary = "사용자 회원가입",
			description = """
					신규 사용자의 회원가입을 진행하고 필수 약관 동의 내역을 함께 저장한다.
					
					- username: 6자 이상 영문소문자로 시작 영문/숫자 조합
					- password: 8자리 이상 영문/숫자/특수문자 각각 하나 이상 비밀번호는 해시후 저장
					- name: 숫자, 특수문자, 공백 불가
					
					회원가입 후 사용자는 PENDING 상태로 저장되며
					관리자 또는 운영자의 승인을 받아야한다.
			"""
	)
	@ApiResponses
	ResponseEntity<ApiResponse<UserSignupCreatedResponse>> createUserSignup(
			@Parameter(description = "회원가입 정보", required = true)
			@Valid
			UserSignupRequest userSignupRequest
	);
	
	@Operation(
			summary = "운영자 등록",
			description = """
					관리자는 운영자를 등록할 수 있습니다.
					운영자이기 때문에 동의서 약관에 동의할 필요가 없습니다.
					
					운영자 등록 후 User는 APPROVED 상태로 저장됩니다.
			"""
	)
	@ApiResponses
	ResponseEntity<ApiResponse<UserAdminCreatedResponse>> createAdmin(
			@Parameter(description = "운영자 등록 정보", required = true)
			@Valid
			UserAdminCreateRequest userAdminCreateRequest
	);
	
	@Operation(
			summary = "회원가입 승인/거절",
			description = """
					관리자 또는 운영자는 회원가입을 승인 또는 거절할 수 있다.
					운영자는 자신의 관리 지역내 사용자만 승인이 가능하다.
					
					승인 후 대상 사용자는 APPROVED 상태가 된다.
					거절 후 대상 사용자는 REJECTED 상태가 된다.
			"""
	)
	ResponseEntity<ApiResponse<UserApprovalResponse>> approval(
			@Parameter(description = "승인/거절 정보", required = true)
			@Valid
			UserApprovalRequest userApprovalRequest,
			
			@Parameter(hidden = true)
			UserContext user
	);
	
	@Operation(
			summary = "퇴원 예정자 등록",
			description = """
					병원 담당자는 퇴원 예정자를 등록할 수 있습니다.
					
					퇴원 예정자의 주소와 지역 ID 가 존재하는 경우 일치 여부를 검증합니다.
					
					생성된 퇴원 예정자는 WITHDRAWN 상태가 됩니다.
			"""
	)
	ResponseEntity<ApiResponse<UserPatientCreatedResponse>> createPatient(
			@Parameter(description = "퇴원 예정자 등록 정보", required = true)
			@Valid
			UserPatientCreateRequest userPatientCreateRequest,
			
			@Parameter(hidden = true)
			UserContext user
	);
}
