package com.todak_todag.provider_service.provider.presentation.controller.api;

import com.todak_todag.provider_service.global.response.ApiResponse;
import com.todak_todag.provider_service.global.response.PageResponse;
import com.todak_todag.provider_service.provider.presentation.response.ProvideServiceSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Provide Service", description = "서비스 종류 API")
public interface ProvideServiceApiSpec {

    @Operation(
            summary = "서비스 종류 목록 조회",
            description = """
                    등록된 서비스 종류 목록을 조회한다.

                    인증된 사용자면 권한에 관계없이 조회할 수 있다.
                    최신 등록순으로 정렬된다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "서비스 종류 목록 조회 성공"
            )
    })
    ApiResponse<PageResponse<ProvideServiceSearchResponse>> search(
            @Parameter(name = "page", description = "페이지 번호 (0부터 시작, 음수는 0으로 보정)")
            Integer page,

            @Parameter(name = "size", description = "페이지 크기 (10, 30, 50 중 하나. 그 외 값은 10으로 보정)")
            Integer size
    );
}
