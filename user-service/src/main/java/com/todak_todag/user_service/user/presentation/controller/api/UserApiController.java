package com.todak_todag.user_service.user.presentation.controller.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.result.UserAdminCreatedResult;
import com.todak_todag.user_service.user.application.result.UserApprovalResult;
import com.todak_todag.user_service.user.application.result.UserPatientCreatedResult;
import com.todak_todag.user_service.user.application.result.UserSignupCreatedResult;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;
import com.todak_todag.user_service.user.application.service.command.UserUpdateService;
import com.todak_todag.user_service.user.presentation.request.UserAdminCreateRequest;
import com.todak_todag.user_service.user.presentation.request.UserApprovalRequest;
import com.todak_todag.user_service.user.presentation.request.UserPatientCreateRequest;
import com.todak_todag.user_service.user.presentation.request.UserSignupRequest;
import com.todak_todag.user_service.user.presentation.request.UserSuspendRequest;
import com.todak_todag.user_service.user.presentation.response.UserAdminCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserApprovalResponse;
import com.todak_todag.user_service.user.presentation.response.UserPatientCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserSignupCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserSuspendedResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class UserApiController implements UserApiSpec {

	private final UserCreateService userCreateService;
	
	private final UserUpdateService userUpdateService;
	
	@Override
	@PostMapping("/users/signup")
	public ResponseEntity<ApiResponse<UserSignupCreatedResponse>> createUserSignup(
			@Valid @RequestBody UserSignupRequest userSignupRequest
	) {
		UserSignupCreatedResult result = userCreateService.createUserSignup(userSignupRequest.toCommand());
		
		UserSignupCreatedResponse response = UserSignupCreatedResponse.of(result);
		
		return ResponseEntity
				.status(201)
				.body(ApiResponse.created("회원가입 신청이 완료되었습니다.", response));
	}

	@Override
	@PostMapping("/admin/users")
	@PreAuthorize("hasRole('MASTER')")
	public ResponseEntity<ApiResponse<UserAdminCreatedResponse>> createAdmin(
			@Valid @RequestBody UserAdminCreateRequest userAdminCreateRequest
	) {
		UserAdminCreatedResult result = userCreateService.createUserAdmin(userAdminCreateRequest.toCommand());
		
		UserAdminCreatedResponse response = new UserAdminCreatedResponse(
				result.userId(),
				result.name(),
				result.province(),
				result.district()
		);
		
		return ResponseEntity
				.status(200)
				.body(ApiResponse.ok("운영자 등록 완료", response));
	}

	@Override
	@PatchMapping("/admin/users/status")
	@PreAuthorize("hasAnyRole('MASTER', 'ADMIN')")
	public ResponseEntity<ApiResponse<UserApprovalResponse>> approval(
			@Valid @RequestBody UserApprovalRequest userApprovalRequest,
			@AuthenticationPrincipal UserContext user
	) {
		
		UserApprovalResult result = userUpdateService.approval(
				userApprovalRequest.toCommand(user)
		);
		
		UserApprovalResponse response = new UserApprovalResponse(
				result.userId(),
				result.role().getKoreaName(),
				result.rejectReason()
		);
		
		String responseMessage = result.isAccept()
				? "회원가입 요청 승인 완료"		// true
				: "회원가입 요청 거절 완료";	// false
		
		return ResponseEntity
				.status(200)
				.body(ApiResponse.ok(responseMessage, response));
	}

	@Override
	@PostMapping("/users/patient")
	@PreAuthorize("hasRole('HOSPITAL_STAFF')")
	public ResponseEntity<ApiResponse<UserPatientCreatedResponse>> createPatient(
			@Valid @RequestBody UserPatientCreateRequest userPatientCreateRequest,
			@AuthenticationPrincipal UserContext user
	) {
		
		UserPatientCreatedResult result = userCreateService.createUserPatient(userPatientCreateRequest.toCommand(user));
		
		UserPatientCreatedResponse response = new UserPatientCreatedResponse(
				result.patientId(),
				result.hospitalStaffId(),
				result.name(),
				result.phone(),
				result.regionId()
		);
		
		return ResponseEntity.status(200).body(ApiResponse.ok("퇴원 예정자 등록 완료", response));
	}

	@Override
	@PatchMapping("/admin/users/{userId}/suspend")
	@PreAuthorize("hasAnyRole('MASTER', 'ADMIN')")
	public ResponseEntity<ApiResponse<UserSuspendedResponse>> suspend(
			@PathVariable("userId") UUID userId,
			@Valid @RequestBody UserSuspendRequest userSuspendRequest,
			@AuthenticationPrincipal UserContext user
	) {
		UUID suspendedUserId = userUpdateService.suspend(userSuspendRequest.toCommand(userId, user));
		
		UserSuspendedResponse response = new UserSuspendedResponse(suspendedUserId);
		
		return ResponseEntity
				.status(200)
				.body(ApiResponse.ok("해당 사용자가 일시 정지 되었습니다.", response));
	}
	
	
}