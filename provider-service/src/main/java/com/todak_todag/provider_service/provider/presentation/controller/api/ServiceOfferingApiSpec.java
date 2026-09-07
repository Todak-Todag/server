package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.response.PageResponse;
import com.todak_todag.provider_service.global.security.UserContext;
import com.todak_todag.provider_service.provider.presentation.request.ServiceOfferingCreateRequest;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingCreateResponse;
import com.todak_todag.provider_service.provider.presentation.response.ServiceOfferingSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.UUID;

@Tag(name = "Service Offering", description = "제공 서비스 API")
public interface ServiceOfferingApiSpec {

    @Operation(
            summary = "제공 서비스 등록",
            description = """
                    서비스 제공자가 자신이 제공할 수 있는 서비스 종류를 등록한다.
                    같은 서비스 종류를 중복으로 등록할 수 없다.
                    등록 시 User-Service에서 제공자의 담당 지역을 조회해 함께 저장하므로, 담당 지역이 없으면 등록할 수 없다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "제공 서비스 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation 실패 또는 담당 지역 미지정"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SERVICE_PROVIDER가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 서비스 종류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 등록한 서비스 종류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "User-Service 호출 실패")
    })
    ApiResponse<ServiceOfferingCreateResponse> create(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(description = "등록할 서비스 종류 정보", required = true)
            @Valid
            ServiceOfferingCreateRequest request
    );

    @Operation(
            summary = "제공 서비스 삭제",
            description = """
                    등록한 제공 서비스를 논리 삭제한다. 하위 제공 가능 요일/시간도 함께 삭제된다.
                    본인이 등록한 제공 서비스만 삭제할 수 있으며, 확정된 서비스 일정이 남아 있으면 삭제할 수 없다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "제공 서비스 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "serviceOfferingId 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 등록한 제공 서비스가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 제공 서비스"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "확정된 서비스 일정이 존재함"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Schedule-Service 호출 실패")
    })
    void delete(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(name = "serviceOfferingId", description = "삭제할 제공 서비스 ID", required = true)
            UUID serviceOfferingId
    );

    @Operation(
            summary = "서비스 제공자별 제공 서비스 목록 조회",
            description = """
                    서비스 제공자는 본인이 등록한 제공 서비스만 조회할 수 있다.
                    providerId는 ADMIN만 사용할 수 있으며, SERVICE_PROVIDER가 본인 외의 값을 전달하면 403이다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제공 서비스 목록 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "providerId 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "타인의 providerId로 조회 시도")
    })
    ApiResponse<PageResponse<ServiceOfferingSearchResponse>> search(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(description = "조회할 서비스 제공자 ID (ADMIN만 사용)")
            UUID providerId,

            @Parameter(description = "페이지 번호 (음수 불가, 기본 0)")
            Integer page,

            @Parameter(description = "페이지 크기 (10/30/50, 이외 값은 10으로 보정)")
            Integer size,

            @Parameter(description = "정렬 (createdAt,asc 또는 createdAt,desc, 기본 createdAt,desc)")
            String sort
    );
}