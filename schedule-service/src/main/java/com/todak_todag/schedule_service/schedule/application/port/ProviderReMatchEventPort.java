package com.todak_todag.schedule_service.schedule.application.port;

import java.time.LocalDate;
import java.util.UUID;

// ProviderReMatched 이벤트 발행 추상화
public interface ProviderReMatchEventPort {

    String EVENT_TYPE = "ProviderReMatched";

    void publish(ProviderReMatchEvent event);

    record ProviderReMatchEvent(
            UUID serviceScheduleId,
            UUID serviceOfferingId,
            LocalDate newDate
    ) {
    }
}
