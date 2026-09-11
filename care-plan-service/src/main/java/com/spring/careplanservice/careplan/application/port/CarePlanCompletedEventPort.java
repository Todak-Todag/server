package com.spring.careplanservice.careplan.application.port;

import com.spring.careplanservice.careplan.application.event.CarePlanCompletionEvent;

public interface CarePlanCompletedEventPort {
    void publish(CarePlanCompletionEvent event);
}
