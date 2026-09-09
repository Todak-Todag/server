package com.spring.careplanservice.careplan.application.event;

import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanOutboxEventCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CarePlanCompletionEventAppender {
    private final CarePlanOutboxEventCommandRepository carePlanOutboxEventCommandRepository;
    private final CarePlanCompletionEventPayloadSerializer carePlanCompletionEventPayloadSerializer;

    /**
     * Care Plan 완료 이벤트를 생성하여 Outbox에 적재한다.
     * 호출한 트랜잭션에 참여하므로 Care Plan 상태 변경이 롤백되면
     * Outbox 이벤트 저장도 함께 롤백된다.
     */
    public void append(CarePlan carePlan) {
        CarePlanCompletionEvent event = new CarePlanCompletionEvent(
                UUID.randomUUID(),
                carePlan.getId(),
                carePlan.getPatientId(),
                Instant.now()
        );

        String payload =
                carePlanCompletionEventPayloadSerializer.serialize(event);

        carePlanOutboxEventCommandRepository.save(
                CarePlanOutboxEvent.create(
                        carePlan.getId(),
                        payload
                )
        );
    }
}
