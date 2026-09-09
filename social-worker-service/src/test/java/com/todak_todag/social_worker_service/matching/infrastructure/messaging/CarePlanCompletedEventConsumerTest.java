package com.todak_todag.social_worker_service.matching.infrastructure.messaging;

import com.todak_todag.social_worker_service.matching.application.event.CarePlanCompletedEvent;
import com.todak_todag.social_worker_service.matching.application.service.command.CarePlanCompletedEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.*;

class CarePlanCompletedEventConsumerTest {

    private CarePlanCompletedEventService carePlanCompletedEventService;
    private CarePlanCompletedEventConsumer consumer;

    @BeforeEach
    void setUp() {

        carePlanCompletedEventService =
                mock(CarePlanCompletedEventService.class);

        consumer =
                new CarePlanCompletedEventConsumer(
                        carePlanCompletedEventService
                );
    }

    @Test
    @DisplayName("CarePlanCompleted 이벤트 수신 시 Application Service로 전달한다")
    void delegatesEventToService() {

        CarePlanCompletedEvent event =
                new CarePlanCompletedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now()
                );

        consumer.consume(
                event
        );

        verify(
                carePlanCompletedEventService,
                times(1)
        ).handle(
                event
        );
    }

    @Test
    @DisplayName("이벤트 처리 중 예외가 발생하면 Consumer에서 예외를 삼키지 않는다")
    void propagatesExceptionForRabbitRetry() {

        CarePlanCompletedEvent event =
                new CarePlanCompletedEvent(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Instant.now()
                );

        doThrow(
                new RuntimeException(
                        "DB 처리 실패"
                )
        ).when(
                carePlanCompletedEventService
        ).handle(
                event
        );

        org.junit.jupiter.api.Assertions.assertThrows(
                RuntimeException.class,
                () ->
                        consumer.consume(
                                event
                        )
        );
    }
}