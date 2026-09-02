package com.todak_todag.schedule_service.schedule.application.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// ProviderReMatchEvent <-> 아웃박스 payload(JSON 문자열) 변환
@Component
@RequiredArgsConstructor
public class ProviderReMatchEventPayloadSerializer {

    private final ObjectMapper objectMapper;

    public String serialize(ProviderReMatchEventPort.ProviderReMatchEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("ProviderReMatched 이벤트 페이로드 직렬화에 실패했습니다.", e);
        }
    }

    public ProviderReMatchEventPort.ProviderReMatchEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, ProviderReMatchEventPort.ProviderReMatchEvent.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("ProviderReMatched 이벤트 페이로드 역직렬화에 실패했습니다.", e);
        }
    }
}
