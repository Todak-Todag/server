package com.todak_todag.schedule_service.schedule.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// CarePlanCompletedEvent <-> 아웃박스 payload(JSON 문자열) 변환
@Component
@RequiredArgsConstructor
public class CarePlanCompletedEventPayloadSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(CarePlanCompletedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("[Schedule] CarePlanCompleted 이벤트 페이로드 직렬화에 실패했습니다.", e);
        }
    }

    public CarePlanCompletedEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, CarePlanCompletedEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("[Schedule] CarePlanCompleted 이벤트 페이로드 역직렬화에 실패했습니다.", e);
        }
    }
}
