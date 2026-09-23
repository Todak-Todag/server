package com.spring.careplanservice.careplan.infrastructure.adapter;


import com.spring.careplanservice.careplan.application.event.CarePlanCompletionEvent;
import com.spring.careplanservice.careplan.application.port.CarePlanCompletedEventPort;
import com.spring.careplanservice.global.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

// CarePlan 완료 이벤트를 실제 RabbitMQ로 발행하는 Adapter
//
// 호출 주체는 CarePlan 상태 변경 트랜잭션이 아니라 Outbox Relay.
// 따라서 이 시점에는 CarePlan COMPLETED 변경과 Outbox INSERT가 이미 커밋된 상태이며,
// 발행 실패는 Outbox의 retryCount를 통해 재시도한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class CarePlanCompletedEventRabbitAdapter implements CarePlanCompletedEventPort {
    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(CarePlanCompletionEvent event) {
        // CorrelationData : 나중에 RabbitMQ가 보내는 ACK가 어떤 메시지에 대한 응답인지 연결해주는 객체
        CorrelationData correlationData = new CorrelationData(event.eventId().toString());

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.CARE_PLAN_COMPLETED_EXCHANGE,
                RabbitMqConfig.CARE_PLAN_COMPLETED_ROUTING_KEY,
                event,
                correlationData
        );

        try {
            // Future : 결과가 지금 당장은 없지만, 나중에 올 결과를 담는 객체
            // TODO - Publisher Confirm 대기 시간은 우선 5초로 설정하고,
            // 운영/부하 테스트에서 ACK 지연 시간을 모니터링한 뒤 조정할 것
            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);

            // ACK가 아니라 NACK이면 예외
            if (!confirm.ack()) {
                throw new IllegalStateException(
                        "RabbitMQ publish NACK: " + confirm.reason()
                );
            }

            // 이 로그가 ACK를 받은 뒤 찍힌다
            log.info(
                    "[CarePlan] CarePlanCompleted 이벤트 발행 확인 eventId={}, carePlanId={}",
                    event.eventId(),
                    event.carePlanId()
            );

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "RabbitMQ publish confirm 대기 중 인터럽트 발생",
                    e
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "RabbitMQ publish confirm 실패",
                    e
            );
        }
    }
}
