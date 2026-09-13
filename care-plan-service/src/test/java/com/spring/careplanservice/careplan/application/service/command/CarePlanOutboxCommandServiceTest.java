package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventType;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanOutboxEventCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class CarePlanOutboxCommandServiceTest {
    UUID outboxEventId = UUID.randomUUID();

    @Mock
    private CarePlanOutboxEventCommandRepository carePlanOutboxEventCommandRepository;

    private CarePlanOutboxCommandService carePlanOutboxCommandService;

    @BeforeEach
    void setUp() {
        carePlanOutboxCommandService = new CarePlanOutboxCommandService(
                carePlanOutboxEventCommandRepository
        );
    }

    @Test
    @DisplayName("RabbitMQ 발행 성공 시 Outbox 이벤트를 SENT 상태로 변경")
    void markSent_success() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId)).willReturn(Optional.of(event));

        carePlanOutboxCommandService.markSent(outboxEventId);

        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.SENT);
        assertThat(event.getPublishedAt()).isNotNull();

        verify(carePlanOutboxEventCommandRepository).save(event);
    }

    @Test
    @DisplayName("RabbitMQ 발행 실패 시 retryCount를 증가시키고 PENDING 상태를 유지")
    void recordFailure_retry() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId)).willReturn(Optional.of(event));

        carePlanOutboxCommandService.recordFailure(
                outboxEventId,
                "RabbitMQ connection failed"
        );

        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PENDING);
        assertThat(event.getLastErrorMessage()).isEqualTo("RabbitMQ connection failed");

        verify(carePlanOutboxEventCommandRepository).save(event);
    }

    @Test
    @DisplayName("RabbitMQ 발행이 3회 실패하면 Outbox 이벤트를 FAILED 상태로 변경")
    void recordFailure_maxRetry_failed() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        carePlanOutboxCommandService.recordFailure(
                outboxEventId,
                "1차 발행 실패"
        );

        carePlanOutboxCommandService.recordFailure(
                outboxEventId,
                "2차 발행 실패"
        );

        carePlanOutboxCommandService.recordFailure(
                outboxEventId,
                "3차 발행 실패"
        );

        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.FAILED);
        assertThat(event.getLastErrorMessage()).isEqualTo("3차 발행 실패");

        verify(carePlanOutboxEventCommandRepository, times(3)).save(event);
    }

    @Test
    @DisplayName("PROCESSING 상태(선점 후 발행 시도 중)에서 recordFailure()를 호출하면 " +
            "1·2회차는 PENDING으로 되돌아가 다음 폴링에서 재시도되고, 3회차는 FAILED로 전환된다")
    void recordFailure_fromProcessing_retriesThenFails() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );
        // claim()으로 선점되어 발행을 시도하던 중이라고 가정
        event.startProcessing();
        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PROCESSING);

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        carePlanOutboxCommandService.recordFailure(outboxEventId, "1차 발행 실패");

        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus())
                .as("1회 실패 후에는 PROCESSING에 머무르지 않고 PENDING으로 돌아가 " +
                        "다음 findPending() 폴링에서 재시도 대상이 되어야 한다")
                .isEqualTo(CarePlanOutboxEventStatus.PENDING);

        // 다음 폴링에서 다시 선점되었다고 가정하고 2번째 실패
        event.startProcessing();
        carePlanOutboxCommandService.recordFailure(outboxEventId, "2차 발행 실패");

        assertThat(event.getRetryCount()).isEqualTo(2);
        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PENDING);

        // 다음 폴링에서 다시 선점되었다고 가정하고 3번째(마지막) 실패
        event.startProcessing();
        carePlanOutboxCommandService.recordFailure(outboxEventId, "3차 발행 실패");

        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.FAILED);
        assertThat(event.getLastErrorMessage()).isEqualTo("3차 발행 실패");
    }

    @Test
    @DisplayName("PENDING 상태의 Outbox 이벤트는 선점(PROCESSING)에 성공한다")
    void claim_pending_success() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        boolean claimed = carePlanOutboxCommandService.claim(outboxEventId);

        assertThat(claimed).isTrue();
        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PROCESSING);
        verify(carePlanOutboxEventCommandRepository).save(event);
    }

    @Test
    @DisplayName("이미 PENDING이 아닌 Outbox 이벤트는 선점에 실패하고 저장을 시도하지 않는다")
    void claim_notPending_returnsFalseWithoutSaving() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );
        event.markSent();

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        boolean claimed = carePlanOutboxCommandService.claim(outboxEventId);

        assertThat(claimed).isFalse();
        verify(carePlanOutboxEventCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("PROCESSING 상태이고 updatedAt이 threshold보다 오래되었으면 PENDING으로 되돌린다")
    void revertStuckProcessing_stuck_success() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );
        event.startProcessing();
        // 실제로는 JPA Auditing(@LastModifiedDate)이 채우는 값이므로,
        // 순수 단위 테스트에서는 리플렉션으로 "오래 전에 갱신됨"을 직접 흉내낸다.
        ReflectionTestUtils.setField(event, "updatedAt", Instant.now().minusSeconds(120));

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        carePlanOutboxCommandService.revertStuckProcessing(
                outboxEventId,
                Instant.now().minusSeconds(60)
        );

        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PENDING);
        verify(carePlanOutboxEventCommandRepository).save(event);
    }

    @Test
    @DisplayName("PROCESSING 상태가 아닌 이벤트는 복구를 시도하지 않는다")
    void revertStuckProcessing_notProcessing_noOp() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );
        // 선점 후 이미 다른 처리로 SENT까지 끝난 상태를 가정
        event.startProcessing();
        event.markSent();
        ReflectionTestUtils.setField(event, "updatedAt", Instant.now().minusSeconds(120));

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        carePlanOutboxCommandService.revertStuckProcessing(
                outboxEventId,
                Instant.now().minusSeconds(60)
        );

        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.SENT);
        verify(carePlanOutboxEventCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("PROCESSING 상태이지만 updatedAt이 threshold 이후(최근 재선점됨)라면 복구하지 않는다 — race condition 방지")
    void revertStuckProcessing_recentlyReclaimed_doesNotRevert() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );
        event.startProcessing();
        // findStuckProcessing() 조회 이후, 다른 인스턴스가 이미 복구하고 재선점해
        // updatedAt이 threshold보다 최신으로 갱신된 상황을 흉내낸다.
        ReflectionTestUtils.setField(event, "updatedAt", Instant.now());

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        carePlanOutboxCommandService.revertStuckProcessing(
                outboxEventId,
                Instant.now().minusSeconds(60)
        );

        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PROCESSING);
        verify(carePlanOutboxEventCommandRepository, never()).save(any());
    }
}
