package com.todak_todag.provider_service.provider.presentation.controller.internal;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingIdsResponse;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingProviderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

@Tag(name = "Internal - Service Offering", description = "제공 서비스 내부 API")
public interface ServiceOfferingInternalApiSpec {

    @Operation(
            summary = "제공 서비스 제공자 조회",
            description = """
                    Schedule-Service가 서비스 일정의 담당 제공자를 확인할 때 호출한다.
                    논리 삭제된 제공 서비스는 조회되지 않는다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제공 서비스 제공자 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "serviceOfferingId 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "X-Internal-Api-Key 없거나 불일치"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 제공 서비스")
    })
    ApiResponse<ServiceOfferingProviderResponse> findProvider(
            @Parameter(name = "serviceOfferingId", description = "조회할 제공 서비스 ID", required = true)
            UUID serviceOfferingId
    );

    @Operation(
            summary = "제공자별 제공 서비스 ID 목록 조회",
            description = """
                    Schedule-Service가 서비스 제공자의 일정 목록을 조회할 때, 요청자가 보유한 제공 서비스 ID를 한 번에 확인하기 위해 호출한다.
                    p_service_schedules에 provider_id가 없어 일정 건마다 단건 조회를 하면 호출이 과도해지므로 제공자 기준으로 묶어 반환한다.
                    논리 삭제된 제공 서비스는 조회되지 않는다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제공 서비스 목록 조회 성공 (없으면 빈 배열)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "providerId 누락 또는 UUID 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "X-Internal-Api-Key 없거나 불일치")
    })
    ApiResponse<ServiceOfferingIdsResponse> findIdsByProvider(
            @Parameter(description = "조회할 서비스 제공자 ID", required = true)
            UUID providerId
    );
}