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
        UUID outboxEventId = UUID.randomUUID();

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
}
