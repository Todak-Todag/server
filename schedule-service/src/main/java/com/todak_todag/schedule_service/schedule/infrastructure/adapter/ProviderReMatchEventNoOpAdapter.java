package com.todak_todag.schedule_service.schedule.infrastructure.adapter;

import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/*
 * TODO: 실제 RabbitMQ 발행 Adapter로 변경
 *  Exchange/Queue/RoutingKey/payload 스펙이 확정된 뒤 해당 클래스 대체
 *  현재는 로그만 남기고 실제 발행은 하지 않음
 *
 */
@Slf4j
@Component
public class ProviderReMatchEventNoOpAdapter implements ProviderReMatchEventPort {

    @Override
    public void publish(ProviderReMatchEvent event) {
        log.warn(
                "[Schedule] ProviderReMatched 이벤트 발행 스펙 미확정으로 실제 발행을 생략합니다 serviceScheduleId={} newDate={}",
                event.serviceScheduleId(), event.newDate()
        );
    }
}
