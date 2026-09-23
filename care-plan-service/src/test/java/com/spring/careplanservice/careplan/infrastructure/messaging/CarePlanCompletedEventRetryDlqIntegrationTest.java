package com.spring.careplanservice.careplan.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.careplanservice.careplan.application.event.CarePlanCompletedEvent;
import com.spring.careplanservice.careplan.application.event.ScheduleStatus;
import com.spring.careplanservice.careplan.application.port.ScheduleResultQueryPort;
import com.spring.careplanservice.careplan.support.IntegrationTestSupport;
import com.spring.careplanservice.global.config.RabbitMqConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

class CarePlanCompletedEventRetryDlqIntegrationTest extends IntegrationTestSupport {
    /*
    CarePlanCompleted Consumer의 Retry / DLQ 통합 테스트
    = Consumer 처리가 계속 실패하는 경우
      - 최초 1회 + Retry 3회, 총 4회까지만 시도되는지 (spring.rabbitmq.listener.simple.retry.max-retries: 3
        기준, Spring Framework 7의 RetryPolicy는 "total attempts = 1 initial attempt + maxRetries attempts")
      - 그 이후 원본 큐로 무한 재큐잉되지 않고 DLQ(care-plan.schedule-completed.dlq.queue)로 이동하는지
      - 원본 큐(care-plan.schedule-completed.queue)에는 해당 메시지가 남지 않는지
    를 실제 Testcontainers RabbitMQ로 검증한다.
    */

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduleResultQueryPort scheduleResultQueryPort;

    @Test
    @DisplayName("Consumer 처리가 계속 실패하면 최초 1회+Retry 3회(총 4회) 이후 원본 큐가 아닌 DLQ로 이동한다")
    void consumeCarePlanCompleted_exhaustsRetryThenDeadLetters() throws Exception {
        UUID carePlanId = UUID.randomUUID();
        UUID serviceResultId = UUID.randomUUID();

        // scheduleResultQueryPort 호출이 매번 실패하도록 만들어
        // completeCarePlan() 처리 중 예외가 항상 발생하는 poison message 상황을 재현한다.
        given(scheduleResultQueryPort.findById(any()))
                .willThrow(new RuntimeException("schedule-service 응답 실패"));

        CarePlanCompletedEvent event = new CarePlanCompletedEvent(
                carePlanId,
                serviceResultId,
                ScheduleStatus.COMPLETED
        );

        // Schedule-Service가 발행하는 것과 동일한 Exchange / Routing Key로 이벤트 발행
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.SCHEDULE_EXCHANGE,
                RabbitMqConfig.SCHEDULE_COMPLETED_ROUTING_KEY,
                event
        );

        // 최초 시도 1회 + Retry 3회 = 총 4회 호출된 뒤 더 이상 재시도되지 않는지 확인
        // (initial-interval 1000ms, multiplier 2 기준 최대 대기 시간을 감안해 넉넉히 대기)
        verify(
                scheduleResultQueryPort,
                timeout(20000).times(4)
        ).findById(serviceResultId);

        // Retry 소진 후 DLQ에 동일한 이벤트가 도착하는지 확인
        Message dlqMessage = rabbitTemplate.receive(
                RabbitMqConfig.CARE_PLAN_SCHEDULE_COMPLETED_DLQ,
                10000
        );

        assertThat(dlqMessage).isNotNull();

        JsonNode dlqPayload = objectMapper.readTree(dlqMessage.getBody());
        assertThat(dlqPayload.get("carePlanId").asText()).isEqualTo(carePlanId.toString());
        assertThat(dlqPayload.get("serviceResultId").asText()).isEqualTo(serviceResultId.toString());

        // 원본 큐에는 더 이상 해당 메시지가 남아 있지 않은지(무한 재큐잉되지 않았는지) 확인
        Message remainingInOriginalQueue = rabbitTemplate.receive(
                RabbitMqConfig.CARE_PLAN_SCHEDULE_COMPLETED_QUEUE,
                1000
        );

        assertThat(remainingInOriginalQueue).isNull();
    }
}
