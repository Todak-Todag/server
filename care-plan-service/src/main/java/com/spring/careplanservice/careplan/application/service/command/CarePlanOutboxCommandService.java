package com.spring.careplanservice.careplan.application.service.command;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventStatus;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanOutboxEventCommandRepository;
import com.spring.careplanservice.global.exception.BusinessException;
import com.spring.careplanservice.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
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

    // Relay가 발행을 시도하기 전에 PENDING -> PROCESSING으로 선점을 시도한다.
    // 이미 다른 인스턴스가 선점(또는 처리 완료)했다면 false를 반환한다.
    // save() 시점에 다른 인스턴스가 먼저 선점해 @Version이 이미 바뀌어 있다면
    // ObjectOptimisticLockingFailureException이 발생하며, 이 트랜잭션 밖(호출부)으로 전파된다.
    @Transactional
    public boolean claim(UUID outboxEventId) {
        CarePlanOutboxEvent event = carePlanOutboxEventCommandRepository
                .findById(outboxEventId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "존재하지 않는 CarePlan Outbox 이벤트입니다. outboxEventId="
                                        + outboxEventId
                        )
                );

        if (!event.isPending()) {
            return false;
        }

        event.startProcessing();

        carePlanOutboxEventCommandRepository.save(event);

        return true;
    }

    // 선점(PROCESSING) 후 오래도록 방치된 이벤트를 PENDING으로 되돌려
    // 다음 폴링에서 다시 시도할 수 있게 한다. 죽은 인스턴스가 선점한 채
    // 영원히 멈춰 있는 상태를 방지하기 위한 유지보수 동작이다.
    //
    // threshold는 findStuckProcessing() 조회에 사용한 것과 동일한 값을 전달해야 한다.
    // 조회 시점과 이 메서드 호출 시점 사이에 다른 인스턴스가 이미 복구 후 재선점했을 수 있으므로,
    // 여기서 다시 한 번 "지금도 여전히 PROCESSING이고 threshold보다 오래되었는지"를 재검증한다.
    @Transactional
    public void revertStuckProcessing(UUID outboxEventId, Instant threshold) {
        CarePlanOutboxEvent event = carePlanOutboxEventCommandRepository
                .findById(outboxEventId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "존재하지 않는 CarePlan Outbox 이벤트입니다. outboxEventId="
                                        + outboxEventId
                        )
                );

        if (!event.isStuckProcessing(threshold)) {
            return;
        }

        event.revertStuckProcessing();

        carePlanOutboxEventCommandRepository.save(event);

        log.warn(
                "[CarePlan] 방치된 PROCESSING Outbox 이벤트를 PENDING으로 복구 outboxEventId={}",
                event.getId()
        );
    }

    // 운영자가 FAILED 이벤트를 재처리 대상(PENDING)으로 되돌린다.
    @Transactional
    public void retryFailed(UUID outboxEventId) {
        CarePlanOutboxEvent event = carePlanOutboxEventCommandRepository
                .findById(outboxEventId)
                .orElseThrow(() ->
                        new BusinessException(
                                ErrorCode.CARE_PLAN_OUTBOX_EVENT_NOT_FOUND
                        )
                );

        if (!event.isFailed()) {
            throw new BusinessException(
                    ErrorCode.CARE_PLAN_OUTBOX_EVENT_RETRY_NOT_ALLOWED
            );
        }

        event.retryFromFailed();

        carePlanOutboxEventCommandRepository.save(event);

        log.info(
                "[CarePlan] FAILED Outbox 이벤트 재처리 요청 outboxEventId={}",
                event.getId()
        );
    }
}
