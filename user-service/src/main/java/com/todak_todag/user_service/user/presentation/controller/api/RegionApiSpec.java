package com.todak_todag.user_service.user.presentation.controller.api;

import com.todak_todag.user_service.global.response.ApiResponse;
import com.todak_todag.user_service.global.response.PageResponse;
import com.todak_todag.user_service.user.presentation.request.RegionFindAdminRequest;
import com.todak_todag.user_service.user.presentation.response.RegionFindAdminResponse;
import com.todak_todag.user_service.user.presentation.response.RegionFindAvailableListResponse;
import com.todak_todag.user_service.user.presentation.response.RegionFindDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Region", description = "지역 API")
public interface RegionApiSpec {

    @Operation(
            summary = "서비스 가능 지역 목록 조회",
            description = "회원가입 시 사용자가 선택할 수 있도록 서비스 지원 중인 지역 목록을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "서비스 가능 지역 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<RegionFindAvailableListResponse>> findAvailableRegions();

    @Operation(
            summary = "관리자 지역 목록 조회",
            description = "관리자가 지역 목록을 조건 검색하고 페이징하여 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "지역 목록 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "조회 조건 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "지역 관리 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류"
            )
    })
    ResponseEntity<ApiResponse<PageResponse<RegionFindAdminResponse>>> findAdminRegions(
            RegionFindAdminRequest request
    );

    @Operation(
            summary = "지역 단건 조회",
            description = "지역 ID를 기준으로 삭제되지 않은 지역의 상세 정보를 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "지역 상세 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지역 ID 형식 오류"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "지역을 찾을 수 없음"
            )
    })
    ResponseEntity<ApiResponse<RegionFindDetailResponse>> findRegion(
            UUID regionId
    );
}