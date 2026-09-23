package com.spring.careplanservice.careplan.infrastructure.adapter;

import com.spring.careplanservice.careplan.application.port.ScheduleResultQueryPort;
import com.spring.careplanservice.careplan.application.result.ScheduleResultFindResult;
import com.spring.careplanservice.careplan.infrastructure.client.ScheduleFeignClient;
import com.spring.careplanservice.careplan.infrastructure.client.ScheduleInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduleResultClientAdapter implements ScheduleResultQueryPort {
    private final ScheduleFeignClient scheduleFeignClient;

    @Override
    public ScheduleResultFindResult findById(UUID serviceResultId) {
        ScheduleInternalResponse scheduleInternalResponse = scheduleFeignClient.findServiceResult(serviceResultId);

        return new ScheduleResultFindResult(
                scheduleInternalResponse.data().serviceResultId(),
                scheduleInternalResponse.data().carePlanId()
        );
    }
}
