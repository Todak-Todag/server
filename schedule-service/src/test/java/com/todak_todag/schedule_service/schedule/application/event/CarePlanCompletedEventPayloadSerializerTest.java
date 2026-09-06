package com.todak_todag.schedule_service.schedule.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 아웃박스 payload(JSON)가 CarePlanCompleted 페이로드 스펙과 일치하는지 검증
class CarePlanCompletedEventPayloadSerializerTest {

    private final CarePlanCompletedEventPayloadSerializer serializer =
            new CarePlanCompletedEventPayloadSerializer(new ObjectMapper());

    @Test
    @DisplayName("직렬화 결과는 serviceResultId와 status 두 필드를 갖는다")
    void 직렬화_결과가_스펙과_일치한다() {
        // given
        UUID serviceResultId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        CarePlanCompletedEvent event = new CarePlanCompletedEvent(serviceResultId, ScheduleStatus.COMPLETED);

        // when
        String payload = serializer.serialize(event);

        // then
        assertThat(payload).isEqualTo(
                "{\"serviceResultId\":\"550e8400-e29b-41d4-a716-446655440000\",\"status\":\"COMPLETED\"}"
        );
    }

    @Test
    @DisplayName("취소로 끝난 케어플랜은 serviceResultId가 null로 직렬화된다")
    void 취소된_경우_resultId가_null로_직렬화된다() {
        // given
        CarePlanCompletedEvent event = new CarePlanCompletedEvent(null, ScheduleStatus.CANCELED);

        // when
        String payload = serializer.serialize(event);

        // then
        assertThat(payload).isEqualTo("{\"serviceResultId\":null,\"status\":\"CANCELED\"}");
    }

    @Test
    @DisplayName("직렬화한 payload를 다시 역직렬화하면 원본 이벤트와 같다")
    void 직렬화_후_역직렬화하면_원본과_같다() {
        // given
        CarePlanCompletedEvent event = new CarePlanCompletedEvent(UUID.randomUUID(), ScheduleStatus.NO_SHOW);

        // when
        CarePlanCompletedEvent restored = serializer.deserialize(serializer.serialize(event));

        // then
        assertThat(restored).isEqualTo(event);
    }

    @Test
    @DisplayName("깨진 payload는 역직렬화에 실패해 예외를 던진다 — 릴레이가 해당 건만 실패로 기록할 수 있다")
    void 잘못된_payload는_예외를_던진다() {
        // given
        String brokenPayload = "{\"serviceResultId\":";

        // when & then
        assertThatThrownBy(() -> serializer.deserialize(brokenPayload))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("역직렬화");
    }
}
