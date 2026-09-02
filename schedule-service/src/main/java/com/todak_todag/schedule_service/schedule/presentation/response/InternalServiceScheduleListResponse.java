package com.todak_todag.schedule_service.schedule.presentation.response;

import com.todak_todag.schedule_service.schedule.application.result.InternalServiceScheduleSearchResult;

import java.util.List;

// [내부 API] 서비스 제공자 일정 조회 응답 목록
public record InternalServiceScheduleListResponse(
        List<InternalServiceScheduleResponse> content
) {
    public static InternalServiceScheduleListResponse of(List<InternalServiceScheduleSearchResult> results) {
        List<InternalServiceScheduleResponse> content = results.stream()
                .map(InternalServiceScheduleResponse::from)
                .toList();

        return new InternalServiceScheduleListResponse(content);
    }
}
