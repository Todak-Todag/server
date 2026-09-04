package com.spring.careplanservice.careplan.application.port;

import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;

public interface CarePlanEventPort {
    void publishCarePlanConfirmed(
            CarePlanConfirmedEvent carePlanConfirmedEvent
    );
}