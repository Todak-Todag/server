package com.spring.careplanservice.careplan.infrastructure.adapter;

import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CarePlanConfirmedEventRabbitAdapterTest {
    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private CarePlanConfirmedEventRabbitAdapter carePlanConfirmedEventRabbitAdapter;

    @Test
    @DisplayName("RabbitMQ ACK 수신 시 이벤트 발행 성공")
    void publish_ack_success() {
        CarePlanConfirmedEvent event = createEvent();

        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);

            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(
                            true,
                            null
                    )
            );

            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                (Object) any(),
                any(CorrelationData.class)
        );

        carePlanConfirmedEventRabbitAdapter.publish(event);

        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                eq(event),
                any(CorrelationData.class)
        );
    }

    @Test
    @DisplayName("RabbitMQ NACK 수신 시 예외 발생")
    void publish_nack_throwsException() {
        CarePlanConfirmedEvent event = createEvent();

        doAnswer(invocation -> {
            CorrelationData correlationData = invocation.getArgument(3);

            correlationData.getFuture().complete(
                    new CorrelationData.Confirm(
                            false,
                            "test nack"
                    )
            );

            return null;
        }).when(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                (Object) any(),
                any(CorrelationData.class)
        );

        assertThatThrownBy(() -> carePlanConfirmedEventRabbitAdapter.publish(event)).isInstanceOf(IllegalStateException.class);

        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                eq(event),
                any(CorrelationData.class)
        );
    }

    @Test
    @DisplayName("Publisher Confirm 응답이 timeout되면 예외 발생")
    void publish_timeout_throwsException() {
        CarePlanConfirmedEvent event = createEvent();

        assertThatThrownBy(() -> carePlanConfirmedEventRabbitAdapter.publish(event)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("RabbitMQ publish confirm 실패");

        verify(rabbitTemplate).convertAndSend(
                anyString(),
                anyString(),
                eq(event),
                any(CorrelationData.class)
        );
    }

    private CarePlanConfirmedEvent createEvent() {
        return new CarePlanConfirmedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of()
        );
    }
}