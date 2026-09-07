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
