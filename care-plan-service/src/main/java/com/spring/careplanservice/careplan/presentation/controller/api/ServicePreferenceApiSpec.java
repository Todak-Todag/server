package com.spring.careplanservice.careplan.presentation.controller.api;

import com.spring.careplanservice.careplan.presentation.request.ServicePreferenceCreateRequest;
import com.spring.careplanservice.careplan.presentation.request.ServicePreferenceUpdateRequest;
import com.spring.careplanservice.careplan.presentation.response.ServicePreferenceCreateResponse;
import com.spring.careplanservice.careplan.presentation.response.ServicePreferenceFindResponse;
import com.spring.careplanservice.careplan.presentation.response.ServicePreferenceUpdateResponse;
import com.spring.careplanservice.global.response.ApiResponse;
import com.spring.careplanservice.global.security.UserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(
        name = "Service Preference",
        description = "Care Plan 서비스 희망 일정 API"
)
public interface ServicePreferenceApiSpec {
    @Operation(
            summary = "서비스 희망 일정 등록",
            description = """
                    퇴원 예정자가 신청한 Care Plan 서비스에 희망 일정을 등록한다.
                    
                    희망 날짜는 현재 날짜 이후이면서
                    Care Plan 서비스 제공 기간 내의 날짜여야 한다.
                    UNDER_REVIEW 상태의 Care Plan에서만 등록할 수 있다.
                    """
    )
    @ApiResponses
    ResponseEntity<ApiResponse<ServicePreferenceCreateResponse>> createServicePreference(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "planServiceId",
                    description = "희망 일정을 등록할 Care Plan 서비스 항목 ID",
                    required = true
            )
            UUID planServiceId,

            @Parameter(description = "서비스 희망 일정 정보", required = true)
            @Valid
            ServicePreferenceCreateRequest servicePreferenceCreateRequest
    );

    @Operation(
            summary = "서비스 희망 일정 수정",
            description = """
                    퇴원 예정자가 등록한 서비스 희망 일정의 날짜와 시간대를 수정한다.
                    
                    희망 날짜는 현재 날짜 이후이면서
                    Care Plan 서비스 제공 기간 내의 날짜여야 한다.
                    UNDER_REVIEW 상태의 Care Plan에서만 수정할 수 있다.
                    """
    )
    @ApiResponses
    ResponseEntity<ApiResponse<ServicePreferenceUpdateResponse>> updateServicePreference(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "servicePreferenceId",
                    description = "수정할 서비스 희망 일정 ID",
                    required = true
            )
            UUID servicePreferenceId,

            @Parameter(description = "수정할 서비스 희망 일정 정보", required = true)
            @Valid
            ServicePreferenceUpdateRequest servicePreferenceUpdateRequest
    );

    @Operation(
            summary = "서비스 희망 일정 삭제",
            description = """
                    퇴원 예정자가 등록한 특정 서비스 희망 일정을 논리 삭제한다.
                    
                    서비스 신청 자체는 유지하며 희망 일정만 삭제한다.
                    UNDER_REVIEW 상태의 Care Plan에서만 삭제할 수 있다.
                    """
    )
    @ApiResponses
    ResponseEntity<Void> deleteServicePreference(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "servicePreferenceId",
                    description = "삭제할 서비스 희망 일정 ID",
                    required = true
            )
            UUID servicePreferenceId
    );

    @Operation(
            summary = "서비스 희망 일정 상세 조회",
            description = """
                    서비스 희망 일정 ID를 기준으로 상세 정보를 조회한다.
                    
                    현재는 퇴원 예정자 본인의 서비스 희망 일정만 조회할 수 있다.
                    """
    )
    @ApiResponses
    ResponseEntity<ApiResponse<ServicePreferenceFindResponse>> findServicePreference(
            @Parameter(hidden = true)
            UserContext user,

            @Parameter(
                    name = "servicePreferenceId",
                    description = "조회할 서비스 희망 일정 ID",
                    required = true
            )
            UUID servicePreferenceId
    );
}
