package com.todak_todag.schedule_service.schedule.application.event;

import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;

import java.time.LocalDate;
import java.util.UUID;

// ProviderReMatched 이벤트 페이로드 (발행)
public record ProviderReMatchEvent(
        UUID carePlanId,
        UUID regionId,
        UUID provideServiceId,
        UUID servicePreferenceId,
        LocalDate date,
        PreferredTimeSlot preferredTimeSlot
) {

    // 재매칭 시도 경로 전용 생성자
    // carePlanId/regionId/provideServiceId/servicePreferenceId는 재시도 대상이 된 기존 실패 매칭 시도 레코드에서 그대로 읽어오고,
    // date/preferredTimeSlot만 사용자가 새로 고른 값으로 채움
    public static ProviderReMatchEvent forRetry(
            UUID carePlanId,
            UUID regionId,
            UUID provideServiceId,
            UUID servicePreferenceId,
            LocalDate date,
            PreferredTimeSlot preferredTimeSlot
    ) {
        return new ProviderReMatchEvent(
                carePlanId,
                regionId,
                provideServiceId,
                servicePreferenceId,
                date,
                preferredTimeSlot
        );
    }

    // 서비스 일정 변경 경로 전용 생성자
    // 일정 변경시에는 TimeSlot을 따로 생성하지 않음
    public static ProviderReMatchEvent forScheduleChange(
            UUID carePlanId,
            UUID regionId,
            UUID provideServiceId,
            UUID servicePreferenceId,
            LocalDate date
    ) {
        return new ProviderReMatchEvent(
                carePlanId,
                regionId,
                provideServiceId,
                servicePreferenceId,
                date,
                null
        );
    }
}
