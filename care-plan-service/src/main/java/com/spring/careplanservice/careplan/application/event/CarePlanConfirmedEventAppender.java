package com.spring.careplanservice.careplan.application.event;

import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEvent;
import com.spring.careplanservice.careplan.domain.entity.CarePlanOutboxEventType;
import com.spring.careplanservice.careplan.domain.repository.command.CarePlanOutboxEventCommandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarePlanConfirmedEventAppender {
    private final CarePlanOutboxEventCommandRepository carePlanOutboxEventCommandRepository;
    private final CarePlanConfirmedEventPayloadSerializer carePlanConfirmedEventPayloadSerializer;

    /**
     * Care Plan 확정 이벤트를 Outbox에 적재한다.
     * 호출한 트랜잭션에 참여하므로 Care Plan 상태 변경이 롤백되면
     * Outbox 이벤트 저장도 함께 롤백된다.
     */
    public void append(CarePlanConfirmedEvent event) {
        String payload =
                carePlanConfirmedEventPayloadSerializer.serialize(event);

        carePlanOutboxEventCommandRepository.save(
                CarePlanOutboxEvent.create(
                        event.carePlanId(),
                        CarePlanOutboxEventType.CARE_PLAN_CONFIRMED,
                        payload
                )
        );
    }
}
