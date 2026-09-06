package com.spring.careplanservice.careplan.application.port;

import com.spring.careplanservice.careplan.application.result.ScheduleResultFindResult;

import java.util.UUID;

public interface ScheduleResultQueryPort {
    ScheduleResultFindResult findById(UUID serviceResultId);
}
