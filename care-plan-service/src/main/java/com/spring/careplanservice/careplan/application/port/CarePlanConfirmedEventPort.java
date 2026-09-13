package com.spring.careplanservice.careplan.application.port;

import com.spring.careplanservice.careplan.application.event.CarePlanConfirmedEvent;

public interface CarePlanConfirmedEventPort {
    void publish(CarePlanConfirmedEvent event);
}
