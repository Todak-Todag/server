package com.todak_todag.user_service.user.presentation.controller.api;

import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.response.PageResponse;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.presentation.request.UserAdminCreateRequest;
import com.todak_todag.user_service.user.presentation.request.UserApprovalRequest;
import com.todak_todag.user_service.user.presentation.request.UserSearchRequest;
import com.todak_todag.user_service.user.presentation.request.UserSuspendRequest;
import com.todak_todag.user_service.user.presentation.response.UserAdminCreatedResponse;
import com.todak_todag.user_service.user.presentation.response.UserApprovalResponse;
import com.todak_todag.user_service.user.presentation.response.UserSearchResponse;
import com.todak_todag.user_service.user.presentation.response.UserSuspendedResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Service User-Admin", description = "관리자 User API")
public interface UserAdminApiSpec {

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
			@Parameter(description = "승인/거절 대상 사용자 ID", required = true)
			UUID userId,
			
			@Parameter(description = "승인/거절 정보", required = true)
			@Valid
			UserApprovalRequest userApprovalRequest,
			
			@Parameter(hidden = true)
			UserContext user
	);
	
	@Operation(
			summary = "사용자 일시 정지",
			description = """
					관리자와 지역별 운영자는 사용자를 일시 정지할 수 있습니다.
					
					정지 사유는 필수이며 지역별 운영자는 자신의 지역내 사용자에 대해서만 일시 정지가 가능합니다.
					
					정지된 사용자는 SUSPENDED 상태가 됩니다.
			"""
	)
	ResponseEntity<ApiResponse<UserSuspendedResponse>> suspend(
			@Parameter(description = "정지 대상 사용자 ID", required = true)
			UUID userId,
			
			@Parameter(description = "정지 사유", required = true)
			@Valid
			UserSuspendRequest userSuspendRequest,
			
			@Parameter(hidden = true)
			UserContext user
	);
	
	@Operation(
			summary = "운영용 사용자 검색",
			description = """
					관리자와 운영자는 조건(권한, 상태)에 맞는 사용자를 검색할 수 있습니다.

					role, status는 내부 권한 체계를 노출하지 않기 위해 숫자 코드를 전달합니다.

					운영자(ADMIN)는 자신의 담당 지역내 사용자만 검색됩니다.
			"""
	)
	ResponseEntity<ApiResponse<PageResponse<UserSearchResponse>>> search(
			@Parameter(description = "검색 조건 (page, size, role, status)")
			@Valid
			UserSearchRequest userSearchRequest,
			
			@Parameter(hidden = true)
			UserContext user
	);
}
