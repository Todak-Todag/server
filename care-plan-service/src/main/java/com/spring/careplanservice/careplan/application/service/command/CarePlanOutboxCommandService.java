package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanOutboxEventCommandRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


// Outbox 이벤트의 발행 결과 상태 변경 담당
@Slf4j
@Service
@RequiredArgsConstructor
public class CarePlanOutboxCommandService {
    private final CarePlanOutboxEventCommandRepository carePlanOutboxEventCommandRepository;

    // RabbitMQ 발행 성공 시 SENT 상태로 변경
    @Transactional
    public void markSent(UUID outboxEventId) {
        CarePlanOutboxEvent event = carePlanOutboxEventCommandRepository
                .findById(outboxEventId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "존재하지 않는 CarePlan Outbox 이벤트입니다. outboxEventId="
                                        + outboxEventId
                        )
                );

        event.markSent();

        carePlanOutboxEventCommandRepository.save(event);

        log.info(
                "[CarePlan] Outbox 이벤트 발행 상태 변경 완료 outboxEventId={} status={}",
                event.getId(),
                event.getStatus()
        );
    }

    // RabbitMQ 발행 실패 횟수와 오류 메시지를 기록
    // 3회 실패 시 엔티티 내부에서 FAILED 상태로 변경
    @Transactional
    public void recordFailure(
            UUID outboxEventId,
            String errorMessage
    ) {
        CarePlanOutboxEvent event = carePlanOutboxEventCommandRepository
                .findById(outboxEventId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "존재하지 않는 CarePlan Outbox 이벤트입니다. outboxEventId="
                                        + outboxEventId
                        )
                );

        event.recordFailure(errorMessage);

        carePlanOutboxEventCommandRepository.save(event);

        log.warn(
                "[CarePlan] Outbox 이벤트 발행 실패 기록 outboxEventId={} retryCount={} status={}",
                event.getId(),
                event.getRetryCount(),
                event.getStatus()
        );

        if (event.getStatus() == CarePlanOutboxEventStatus.FAILED) {
            log.error(
                    "[CarePlan] Outbox 이벤트 최종 발행 실패 outboxEventId={} retryCount={}",
                    event.getId(),
                    event.getRetryCount()
            );
        }
    }
}
