package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventType;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanOutboxEventCommandRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    @DisplayName("PROCESSING 상태로 방치된 이벤트를 PENDING으로 되돌린다")
    void revertStuckProcessing_success() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );
        event.startProcessing();

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        carePlanOutboxCommandService.revertStuckProcessing(outboxEventId);

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

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        carePlanOutboxCommandService.revertStuckProcessing(outboxEventId);

        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.SENT);
        verify(carePlanOutboxEventCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("FAILED 상태의 Outbox 이벤트를 재처리 대상(PENDING)으로 되돌리고 retryCount/에러메시지를 초기화한다")
    void retryFailed_success() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );
        event.recordFailure("1차 실패");
        event.recordFailure("2차 실패");
        event.recordFailure("3차 실패");

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        carePlanOutboxCommandService.retryFailed(outboxEventId);

        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(0);
        assertThat(event.getLastErrorMessage()).isNull();
        verify(carePlanOutboxEventCommandRepository).save(event);
    }

    @Test
    @DisplayName("FAILED 상태가 아닌 Outbox 이벤트를 재처리하려 하면 예외가 발생하고 상태가 변경되지 않는다")
    void retryFailed_notFailed_throwsException() {
        CarePlanOutboxEvent event = CarePlanOutboxEvent.create(
                UUID.randomUUID(),
                CarePlanOutboxEventType.CARE_PLAN_COMPLETED,
                "{}"
        );

        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.of(event));

        assertThatThrownBy(() -> carePlanOutboxCommandService.retryFailed(outboxEventId))
                .isInstanceOf(BusinessException.class);

        assertThat(event.getStatus()).isEqualTo(CarePlanOutboxEventStatus.PENDING);
        verify(carePlanOutboxEventCommandRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 Outbox 이벤트를 재처리하려 하면 예외가 발생한다")
    void retryFailed_notFound_throwsException() {
        given(carePlanOutboxEventCommandRepository.findById(outboxEventId))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> carePlanOutboxCommandService.retryFailed(outboxEventId))
                .isInstanceOf(BusinessException.class);
    }
}
