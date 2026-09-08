package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanOutboxEventCommandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
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
}
