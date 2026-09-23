package com.todak_todag.social_worker_service.matching.infrastructure.messaging;

import com.todak_todag.social_worker_service.matching.application.event.CarePlanCompletedEvent;
import com.todak_todag.social_worker_service.matching.application.service.command.CarePlanCompletedEventService;
import com.todak_todag.social_worker_service.matching.application.support.idempotency.EventIdempotencyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CarePlanCompletedEventConsumerTest {

    private CarePlanCompletedEventService carePlanCompletedEventService;
    private EventIdempotencyStore eventIdempotencyStore;
    private CarePlanCompletedEventConsumer consumer;

    @BeforeEach
    void setUp() {

        carePlanCompletedEventService =
                mock(CarePlanCompletedEventService.class);

        eventIdempotencyStore =
                mock(EventIdempotencyStore.class);

        consumer =
                new CarePlanCompletedEventConsumer(
                        carePlanCompletedEventService,
                        eventIdempotencyStore
                );
    }

    @Test
    @DisplayName("처리되지 않은 CarePlanCompleted 이벤트는 Application Service로 전달한다")
    void delegatesEventToService() {

        CarePlanCompletedEvent event =
                createEvent();

        when(
                eventIdempotencyStore.tryAcquire(
                        event.eventId()
                )
        ).thenReturn(true);

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
    @DisplayName("이미 처리 중이거나 처리된 이벤트는 중복 처리하지 않는다")
    void duplicateEventIsIgnored() {

        CarePlanCompletedEvent event =
                createEvent();

        when(
                eventIdempotencyStore.tryAcquire(
                        event.eventId()
                )
        ).thenReturn(false);

        consumer.consume(
                event
        );

        verifyNoInteractions(
                carePlanCompletedEventService
        );
    }

    @Test
    @DisplayName("이벤트 처리 실패 시 처리권을 해제하고 예외를 전파한다")
    void releasesEventWhenProcessingFails() {

        CarePlanCompletedEvent event =
                createEvent();

        when(
                eventIdempotencyStore.tryAcquire(
                        event.eventId()
                )
        ).thenReturn(true);

        doThrow(
                new RuntimeException(
                        "DB 처리 실패"
                )
        ).when(
                carePlanCompletedEventService
        ).handle(
                event
        );

        assertThrows(
                RuntimeException.class,
                () -> consumer.consume(
                        event
                )
        );

        verify(
                eventIdempotencyStore,
                times(1)
        ).release(
                event.eventId()
        );
    }

    private CarePlanCompletedEvent createEvent() {

        return new CarePlanCompletedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Instant.now()
        );
    }
}