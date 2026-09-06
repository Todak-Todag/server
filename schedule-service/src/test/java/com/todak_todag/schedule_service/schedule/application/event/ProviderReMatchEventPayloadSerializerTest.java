package com.todak_todag.schedule_service.schedule.application.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.todak_todag.schedule_service.schedule.domain.entity.PreferredTimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// ProviderReMatched 이벤트 페이로드 구성/직렬화 단위 테스트
// 검증 대상: api/12_이벤트발행_ProviderRematched.md 페이로드 표와 실제 JSON이 일치하는지
class ProviderReMatchEventPayloadSerializerTest {

    // Boot가 구성하는 ObjectMapper와 같은 설정으로 맞춘다
    // (JavaTimeModule 등록 + WRITE_DATES_AS_TIMESTAMPS 비활성 → LocalDate가 배열이 아닌 "2026-09-02"로 직렬화)
    private final ProviderReMatchEventPayloadSerializer serializer = new ProviderReMatchEventPayloadSerializer(
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    );

    @Test
    @DisplayName("직렬화한 뒤 역직렬화하면 원래 이벤트와 동일하다")
    void 직렬화한_뒤_역직렬화하면_원래_이벤트와_동일하다() {
        // given — 시간대까지 재선택하는 경로(재매칭 시도 API)를 가정한 전체 필드 페이로드
        ProviderReMatchEvent event = new ProviderReMatchEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalDate.now().plusDays(1),
                PreferredTimeSlot.MORNING
        );

        // when
        String payload = serializer.serialize(event);
        ProviderReMatchEvent restored = serializer.deserialize(payload);

        // then
        assertThat(restored).isEqualTo(event);
    }

    @Test
    @DisplayName("03번 API 경로로 만든 이벤트는 preferredTimeSlot이 null이다")
    void 일정_변경_경로는_시간대가_null이다() {
        // given — 03번은 날짜만 다시 고르고 시간대는 재선택하지 않는다 (12번 문서 발행 시나리오 2번)
        UUID carePlanId = UUID.randomUUID();
        UUID regionId = UUID.randomUUID();
        UUID provideServiceId = UUID.randomUUID();
        UUID servicePreferenceId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 2);

        // when
        ProviderReMatchEvent event = ProviderReMatchEvent.forScheduleChange(
                carePlanId, regionId, provideServiceId, servicePreferenceId, date
        );

        // then
        assertThat(event.preferredTimeSlot()).isNull();
        assertThat(event.carePlanId()).isEqualTo(carePlanId);
        assertThat(event.regionId()).isEqualTo(regionId);
        assertThat(event.provideServiceId()).isEqualTo(provideServiceId);
        assertThat(event.servicePreferenceId()).isEqualTo(servicePreferenceId);
        assertThat(event.date()).isEqualTo(date);
    }

    @Test
    @DisplayName("직렬화 결과의 키 구성과 순서가 문서의 Example JSON과 일치한다")
    void 직렬화_결과가_문서_스펙과_일치한다() {
        // given — 12번 문서 Example의 값을 그대로 사용
        ProviderReMatchEvent event = ProviderReMatchEvent.forScheduleChange(
                UUID.fromString("a83e5c17-2d69-47f4-b901-8c6a3e5d7f20"),
                UUID.fromString("6b2f9d41-c857-4e13-a690-5d8b2c7f1e34"),
                UUID.fromString("94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43"),
                UUID.fromString("3f2504e0-4f89-41d3-9a0c-0305e82c3301"),
                LocalDate.of(2026, 9, 2)
        );

        // when
        String payload = serializer.serialize(event);

        // then
        assertThat(payload).isEqualTo(
                "{\"carePlanId\":\"a83e5c17-2d69-47f4-b901-8c6a3e5d7f20\","
                        + "\"regionId\":\"6b2f9d41-c857-4e13-a690-5d8b2c7f1e34\","
                        + "\"provideServiceId\":\"94d8e2b6-3f71-4a95-c608-1e7b5d9f2a43\","
                        + "\"servicePreferenceId\":\"3f2504e0-4f89-41d3-9a0c-0305e82c3301\","
                        + "\"date\":\"2026-09-02\","
                        + "\"preferredTimeSlot\":null}"
        );
    }

    @Test
    @DisplayName("잘못된 형식의 payload를 역직렬화하면 예외를 던진다")
    void 잘못된_형식의_payload를_역직렬화하면_예외를_던진다() {
        // when & then
        assertThatThrownBy(() -> serializer.deserialize("이건 JSON이 아닙니다"))
                .isInstanceOf(IllegalStateException.class);
    }
}
