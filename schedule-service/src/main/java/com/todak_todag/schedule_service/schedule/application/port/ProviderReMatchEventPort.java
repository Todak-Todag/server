package com.todak_todag.schedule_service.schedule.application.port;

import com.todak_todag.schedule_service.schedule.application.event.ProviderReMatchEvent;

// ProviderReMatched 이벤트 발행 추상화
public interface ProviderReMatchEventPort {

    // 아웃박스 레코드의 event_type 컬럼에 저장되는 값
    String EVENT_TYPE = "ProviderReMatched";

    void publish(ProviderReMatchEvent event);
}
