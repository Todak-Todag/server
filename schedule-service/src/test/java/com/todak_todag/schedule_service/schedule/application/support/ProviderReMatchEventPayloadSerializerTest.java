package com.todak_todag.schedule_service.schedule.application.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.todak_todag.schedule_service.schedule.application.port.ProviderReMatchEventPort;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderReMatchEventPayloadSerializerTest {

    private final ProviderReMatchEventPayloadSerializer serializer =
            new ProviderReMatchEventPayloadSerializer(new ObjectMapper().registerModule(new JavaTimeModule()));

    @Test
    void 직렬화한_뒤_역직렬화하면_원래_이벤트와_동일하다() {
        // given
        ProviderReMatchEventPort.ProviderReMatchEvent event = new ProviderReMatchEventPort.ProviderReMatchEvent(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1)
        );

        // when
        String payload = serializer.serialize(event);
        ProviderReMatchEventPort.ProviderReMatchEvent restored = serializer.deserialize(payload);

        // then
        assertThat(restored).isEqualTo(event);
    }

    @Test
    void 잘못된_형식의_payload를_역직렬화하면_예외를_던진다() {
        // when & then
        assertThatThrownBy(() -> serializer.deserialize("이건 JSON이 아닙니다"))
                .isInstanceOf(IllegalStateException.class);
    }
}
