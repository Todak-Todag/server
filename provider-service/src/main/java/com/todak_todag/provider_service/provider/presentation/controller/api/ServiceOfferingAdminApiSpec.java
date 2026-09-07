package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.response.PageResponse;
import com.todak_todag.provider_service.global.security.UserContext;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingRegionSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@Tag(name = "Service Offering - Admin", description = "지역별 제공 서비스 관리 API")
public interface ServiceOfferingAdminApiSpec {

    @Operation(
            summary = "지역별 제공 서비스 전체 목록 조회",
            description = """
                    특정 지역에 등록된 제공 서비스를 전부 조회한다.
                    MASTER는 지역 제한 없이 조회할 수 있고, ADMIN은 자신의 담당 지역만 조회할 수 있다.
                    담당 지역이 지정되지 않은 ADMIN은 조회할 수 없다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "지역별 제공 서비스 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "regionId 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "담당 지역이 아니거나 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "User-Service 호출 실패")
    })
    ApiResponse<PageResponse<ServiceOfferingRegionSearchResponse>> searchByRegion(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(name = "regionId", description = "조회할 지역 ID", required = true)
            UUID regionId,

            @Parameter(description = "페이지 번호 (음수 불가, 기본 0)")
            Integer page,

            @Parameter(description = "페이지 크기 (10/30/50, 이외 값은 10으로 보정)")
            Integer size,

            @Parameter(description = "정렬 (createdAt,asc 또는 createdAt,desc, 기본 createdAt,desc)")
            String sort
    );
}