package com.todak_todag.user_service.user.presentation.controller.internal;

import java.util.Set;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.user.presentation.response.UserInternalReadResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Service Internal User", description = "내부 유저 API")
public interface UserInternalSpec {

    @Operation(
            summary = "내부 사용자 조회",
            description = """
                내부 서비스가 호출하는 사용자 조회입니다.
                
                APPROVED 상태의 회원만 조회됩니다.
            """
    )
    ResponseEntity<ApiResponse<UserInternalReadResponse>> readUser(
    		@Parameter(description = "조회할 사용자 ID", required = true)
    		UUID userId
    );
    
    @Operation(
    		summary = "매칭 가능한 사회복지사 조회",
    		description = """
    				내부 서비스가 호출하는 매칭 가능한 사회복지사를 조회합니다.
    				
    				APPROVED 상태의 회원만 조회되며 퇴원 예정자와 동일한 지역의 사회복지사 리스트를 반환합니다.		
    		"""
    )
    ResponseEntity<ApiResponse<Set<UUID>>> readMatchableSocialWorkers(
    		@Parameter(description = "퇴원 예정자의 사용자 ID", required = true)
    		UUID patientId
    );
}
