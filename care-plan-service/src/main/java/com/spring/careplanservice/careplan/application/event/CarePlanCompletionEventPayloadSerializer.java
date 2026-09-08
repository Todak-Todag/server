package com.spring.careplanservice.careplan.application.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarePlanCompletionEventPayloadSerializer {
    private final ObjectMapper objectMapper;

    // CarePlan 완료 이벤트를 Outbox payload 저장용 JSON 문자열로 변환
    // TODO: Outbox 이벤트 종류가 늘어날 경우
    //       공통 EventPayloadSerializer 추상화 여부 검토
    public String serialize(
            CarePlanCompletionEvent event
    ) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "[CarePlan] CarePlanCompleted 이벤트 payload 직렬화 실패",
                    e
            );
        }
    }
}
