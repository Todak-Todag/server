package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.InternalServiceResultDetailResult;

import java.util.UUID;

// [내부 API] 서비스 수행 결과 조회 응답
public record InternalServiceResultResponse(
        UUID serviceResultId
) {

    public static InternalServiceResultResponse from(InternalServiceResultDetailResult result) {
        return new InternalServiceResultResponse(result.serviceResultId());
    }
}
