package com.todak_todag.user_service.user.presentation.controller.api;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.response.PageResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.result.UserAdminCreatedResult;
import com.todak_todag.user_service.user.application.result.UserApprovalResult;
import com.todak_todag.user_service.user.application.result.UserSearchResult;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;
import com.todak_todag.user_service.user.application.service.command.UserUpdateService;
import com.todak_todag.user_service.user.application.service.query.UserQueryService;
import com.todak_todag.user_service.user.presentation.request.UserAdminCreateRequest;
import com.todak_todag.user_service.user.presentation.request.UserApprovalRequest;
import com.todak_todag.user_service.user.presentation.request.UserSearchRequest;
import com.todak_todag.user_service.user.presentation.request.UserSuspendRequest;
import com.todak_todag.user_service.user.presentation.response.UserAdminCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserApprovalResponse;
import com.todak_todag.user_service.user.presentation.response.UserSearchResponse;
import com.todak_todag.user_service.user.presentation.response.UserSuspendedResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Validated
public class UserAdminApiController implements UserAdminApiSpec {
	
	private final UserCreateService userCreateService;
	
	private final UserUpdateService userUpdateService;
	
	private final UserQueryService userQueryService;

	@Override
	@PostMapping
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
	@PatchMapping("/status")
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
	@PatchMapping("/{userId}/suspend")
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
	
	@Override
	@GetMapping("/search")
	@PreAuthorize("hasAnyRole('MASTER', 'ADMIN')")
	public ResponseEntity<ApiResponse<PageResponse<UserSearchResponse>>> search(
			@Valid @ModelAttribute UserSearchRequest userSearchRequest,
			@AuthenticationPrincipal UserContext user
	) {
		Page<UserSearchResult> result = userQueryService.search(userSearchRequest.toQuery(), user);
		
		return ResponseEntity
				.status(200)
				.body(ApiResponse.ok(
						"사용자 검색 성공",
						PageResponse.of(result, UserSearchResponse::from))
				);
	}
	
}
