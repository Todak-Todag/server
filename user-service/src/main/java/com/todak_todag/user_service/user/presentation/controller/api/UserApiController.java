package com.todak_todag.user_service.user.presentation.controller.api;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.result.UserPatientCreatedResult;
import com.todak_todag.user_service.user.application.result.UserSignupCreatedResult;
import com.todak_todag.user_service.user.application.result.UserUpdateResult;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;
import com.todak_todag.user_service.user.application.service.command.UserUpdateService;
import com.todak_todag.user_service.user.application.service.query.UserQueryService;
import com.todak_todag.user_service.user.application.service.result.UserInfoResult;
import com.todak_todag.user_service.user.presentation.cookie.CookieProvider;
import com.todak_todag.user_service.user.presentation.request.UserDeleteRequest;
import com.todak_todag.user_service.user.presentation.request.UserPasswordUpdateRequest;
import com.todak_todag.user_service.user.presentation.request.UserPatientCreateRequest;
import com.todak_todag.user_service.user.presentation.request.UserSignupRequest;
import com.todak_todag.user_service.user.presentation.request.UserUpdateRequest;
import com.todak_todag.user_service.user.presentation.response.UserInfoResponse;
import com.todak_todag.user_service.user.presentation.response.UserPasswordUpdateResponse;
import com.todak_todag.user_service.user.presentation.response.UserPatientCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserSignupCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserUpdateResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/users")
@Validated
public class UserApiController implements UserApiSpec {

	private final String accessTokenCookieName;
	
	private final String refreshTokenCookieName;
	
	private final CookieProvider cookieProvider;
	
	private final UserCreateService userCreateService;
	
	private final UserUpdateService userUpdateService;
	
	private final UserQueryService userQueryService;
	
	public UserApiController(
			@Value("${authentication.access-token.cookie-name}") String accessTokenCookieName,
			@Value("${authentication.refresh-token.cookie-name}") String refreshTokenCookieName,
			CookieProvider cookieProvider,
			UserCreateService userCreateService,
			UserUpdateService userUpdateService,
			UserQueryService userQueryService
	) {
		this.accessTokenCookieName = accessTokenCookieName;
		this.refreshTokenCookieName = refreshTokenCookieName;
		this.cookieProvider = cookieProvider;
		this.userCreateService = userCreateService;
		this.userUpdateService = userUpdateService;
		this.userQueryService = userQueryService;
	}
	
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
	@PatchMapping("/me/password")
	public ResponseEntity<ApiResponse<UserPasswordUpdateResponse>> passwordUpdate(
			@Valid @RequestBody UserPasswordUpdateRequest userPasswordUpdateRequest,
			@AuthenticationPrincipal UserContext user,
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse
	) {
		String accessToken = cookieProvider.getCookieValue(accessTokenCookieName, servletRequest);
		
		UUID userId = userUpdateService.passwordUpdate(userPasswordUpdateRequest.toCommand(accessToken, user));
		
		cookieProvider.addCookie(accessTokenCookieName, Duration.ZERO, "", servletResponse);
		cookieProvider.addCookie(refreshTokenCookieName, Duration.ZERO, "", servletResponse);
		
		UserPasswordUpdateResponse response = new UserPasswordUpdateResponse(userId);
		
		return ResponseEntity
				.status(200)
				.body(ApiResponse.ok("비밀번호가 변경되었습니다.", response));
	}

	@Override
	@PatchMapping("/me")
	public ResponseEntity<ApiResponse<UserUpdateResponse>> userUpdate(
			@Valid @RequestBody UserUpdateRequest userUpdateRequest,
			@AuthenticationPrincipal UserContext user
	) {
		UserUpdateResult result = userUpdateService.userUpdate(userUpdateRequest.toCommand(user));
		
		UserUpdateResponse response = new UserUpdateResponse(
				result.userId(),
				result.name(),
				result.phone(),
				result.regionId(),
				result.address()
		);
		
		return ResponseEntity
				.status(200)
				.body(ApiResponse.ok("회원정보가 수정되었습니다.", response));
	}

	@Override
	@DeleteMapping("/me")
	public ResponseEntity<ApiResponse<Void>> userDelete(
			@Valid @RequestBody UserDeleteRequest userDeleteRequest,
			@AuthenticationPrincipal UserContext user,
			HttpServletRequest request,
			HttpServletResponse response
	) {
		
		String accessToken = cookieProvider.getCookieValue(accessTokenCookieName, request);
		
		userUpdateService.userDelete(userDeleteRequest.toCommand(user, accessToken));
		
		cookieProvider.addCookie(accessTokenCookieName, Duration.ZERO, "", response);
		cookieProvider.addCookie(refreshTokenCookieName, Duration.ZERO, "", response);
		
		return ResponseEntity.noContent().build();
	}
	
}