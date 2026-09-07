package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.security.UserContext;
import com.todak_todag.provider_service.provider.presentation.request.ProvideWorkCreateRequest;
import com.todak_todag.provider_service.provider.presentation.request.ProvideWorkUpdateRequest;
import com.todak_todag.provider_service.provider.presentation.response.ProvideWorkCreateResponse;
import com.todak_todag.provider_service.provider.presentation.response.ProvideWorkUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.UUID;

@Tag(name = "Provide Work", description = "제공 가능 요일/시간 API")
public interface ProvideWorkApiSpec {

    @Operation(
            summary = "제공 가능 요일/시간 등록",
            description = """
                    서비스 제공자가 제공 서비스에 대한 제공 가능 요일과 시간을 등록한다.
                    요일은 1(월)~7(일)이며 종료 시간은 시작 시간보다 늦어야 한다.
                    같은 제공 서비스 안에서 요일이 같고 시간이 겹치는 일정은 등록할 수 없다. 경계가 닿는 것은 겹침으로 보지 않는다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "제공 가능 일정 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요일 범위 오류 또는 시간 범위 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 등록한 제공 서비스가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 제공 서비스"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "시간이 겹치는 제공 가능 일정 존재")
    })
    ApiResponse<ProvideWorkCreateResponse> create(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(name = "serviceOfferingId", description = "제공 서비스 ID", required = true)
            UUID serviceOfferingId,

            @Parameter(description = "제공 가능 요일/시간 정보", required = true)
            @Valid
            ProvideWorkCreateRequest request
    );

    @Operation(
            summary = "제공 가능 요일/시간 수정",
            description = """
                    등록한 제공 가능 요일과 시간을 수정한다.
                    확정된 서비스 일정이 남아 있으면 수정할 수 없다.
                    겹침 검증 시 자기 자신은 대상에서 제외한다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "제공 가능 일정 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요일 범위 오류 또는 시간 범위 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 등록한 제공 서비스가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 제공 서비스 또는 제공 가능 일정"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "시간 겹침 또는 확정된 서비스 일정 존재"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Schedule-Service 호출 실패")
    })
    ApiResponse<ProvideWorkUpdateResponse> update(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(name = "serviceOfferingId", description = "제공 서비스 ID", required = true)
            UUID serviceOfferingId,

            @Parameter(name = "provideWorkId", description = "수정할 제공 가능 일정 ID", required = true)
            UUID provideWorkId,

            @Parameter(description = "수정할 제공 가능 요일/시간 정보", required = true)
            @Valid
            ProvideWorkUpdateRequest request
    );

    @Operation(
            summary = "제공 가능 요일/시간 삭제",
            description = """
                    등록한 제공 가능 요일과 시간을 논리 삭제한다.
                    확정된 서비스 일정이 남아 있으면 삭제할 수 없다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "제공 가능 일정 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "ID 형식 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인이 등록한 제공 서비스가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 제공 서비스 또는 제공 가능 일정"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "확정된 서비스 일정 존재"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "Schedule-Service 호출 실패")
    })
    void delete(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(name = "serviceOfferingId", description = "제공 서비스 ID", required = true)
            UUID serviceOfferingId,

            @Parameter(name = "provideWorkId", description = "삭제할 제공 가능 일정 ID", required = true)
            UUID provideWorkId
    );
}