package com.todak_todag.provider_service.provider.presentation.controller.internal;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.provider.presentation.response.ProvideServiceInfoListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Tag(name = "Internal - Provide Service", description = "서비스 종류 내부 API")
public interface ProvideServiceInternalApiSpec {

    @Operation(
            summary = "서비스 종류 목록 조회",
            description = """
                    Care-Plan-Service가 신청 서비스 목록·단건을 조회할 때 서비스명과 내용을 채우기 위해 호출한다.
                    여러 ID를 전달하면 목록으로, 하나만 전달하면 단건으로 사용한다.
                    존재하지 않는 ID는 제외하고 조회된 것만 반환하며, 요청 ID와 응답 ID의 대조는 호출하는 쪽이 담당한다.
                    논리 삭제된 서비스 종류는 조회되지 않는다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "서비스 종류 목록 조회 성공 (없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "provideServiceIds 누락 또는 UUID 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "X-Internal-Api-Key 없거나 불일치")
    })
    ApiResponse<ProvideServiceInfoListResponse> findAllByIds(
            @Parameter(name = "provideServiceIds", description = "조회할 서비스 종류 ID 목록 (콤마 구분, 1개 이상)", required = true)
            List<UUID> provideServiceIds
    );
}