package com.spring.careplanservice.careplan.infrastructure.messaging;

import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;
import com.spring.careplanservice.careplan.application.port.CarePlanEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class CarePlanConfirmedEventListener {
    private final CarePlanEventPort carePlanEventPort;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            CarePlanConfirmedEvent carePlanConfirmedEvent
    ) {
        carePlanEventPort.publishCarePlanConfirmed(
                carePlanConfirmedEvent
        );
    }
}
