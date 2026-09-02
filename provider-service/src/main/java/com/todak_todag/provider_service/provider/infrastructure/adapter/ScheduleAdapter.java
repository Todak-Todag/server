package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.provider.application.port.SchedulePort;
import com.todak_todag.provider_service.provider.infrastructure.client.ScheduleClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduleAdapter implements SchedulePort {

    private final ScheduleClient scheduleClient;

    @Override
    public boolean existsConfirmedSchedule(UUID serviceOfferingId) {
        return !scheduleClient
                .findSchedules(List.of(serviceOfferingId), LocalDate.now())
                .data()
                .content()
                .isEmpty();
    }
}