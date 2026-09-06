package com.todak_todag.user_service.user.presentation.controller.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.result.UserPatientCreatedResult;
import com.todak_todag.user_service.user.application.result.UserSignupCreatedResult;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;
import com.todak_todag.user_service.user.application.service.command.UserUpdateService;
import com.todak_todag.user_service.user.application.service.query.UserQueryService;
import com.todak_todag.user_service.user.application.service.result.UserInfoResult;
import com.todak_todag.user_service.user.presentation.request.UserPasswordUpdateRequest;
import com.todak_todag.user_service.user.presentation.request.UserPatientCreateRequest;
import com.todak_todag.user_service.user.presentation.request.UserSignupRequest;
import com.todak_todag.user_service.user.presentation.request.UserSuspendRequest;
import com.todak_todag.user_service.user.presentation.response.UserInfoResponse;
import com.todak_todag.user_service.user.presentation.response.UserPasswordUpdateResponse;
import com.todak_todag.user_service.user.presentation.response.UserPatientCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserSignupCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserSuspendedResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserApiController implements UserApiSpec {

	private final UserCreateService userCreateService;
	
	private final UserUpdateService userUpdateService;
	
	private final UserQueryService userQueryService;
	
	@Override
	@PostMapping("/signup")
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
	@GetMapping("/me")
	public ResponseEntity<ApiResponse<UserInfoResponse>> me(
			@AuthenticationPrincipal UserContext user
	) {
		UserInfoResult result = userQueryService.getMe(user.getUserId());
		
		UserInfoResponse response = UserInfoResponse.from(result);
		
		return ResponseEntity
				.status(200)
				.body(ApiResponse.ok("내 정보 조회 완료", response));

	}
	
	@PostMapping("/patient")
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
	@PatchMapping("/users/me/password")
	public ResponseEntity<ApiResponse<UserPasswordUpdateResponse>> passwordUpdate(
			@Valid @RequestBody UserPasswordUpdateRequest userPasswordUpdateRequest,
			@AuthenticationPrincipal UserContext user
	) {
		UUID userId = userUpdateService.passwordUpdate(userPasswordUpdateRequest.toCommand(user));
		
		UserPasswordUpdateResponse response = new UserPasswordUpdateResponse(userId);
		
		return ResponseEntity
				.status(200)
				.body(ApiResponse.ok("비밀번호가 변경되었습니다.", response));
	}
	
}