package com.spring.careplanservice.careplan.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarePlanConfirmedEventPayloadSerializer {
    private final ObjectMapper objectMapper;

    // CarePlanConfirmed 이벤트를 Outbox payload 저장용 JSON 문자열로 변환
    public String serialize(
            CarePlanConfirmedEvent event
    ) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "[CarePlan] CarePlanConfirmed 이벤트 payload 직렬화 실패",
                    e
            );
        }
    }

    public CarePlanConfirmedEvent deserialize(
            String payload
    ) {
        try {
            return objectMapper.readValue(
                    payload,
                    CarePlanConfirmedEvent.class
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "[CarePlan] CarePlanConfirmed 이벤트 payload 역직렬화 실패",
                    e
            );
        }
    }
}
