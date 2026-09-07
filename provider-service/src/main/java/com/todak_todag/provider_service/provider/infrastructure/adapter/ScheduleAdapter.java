package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.port.SchedulePort;
import com.todak_todag.provider_service.provider.application.port.ScheduleSlot;
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
        ScheduleClient.ServiceScheduleListResponse response = scheduleClient
                .findSchedules(List.of(serviceOfferingId), LocalDate.now())
                .data();

        if (response == null || response.content() == null) {
            throw new BusinessException(ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        return !response.content().isEmpty();
    }

    @Override
    public List<ScheduleSlot> findSchedules(List<UUID> serviceOfferingIds, LocalDate startDate) {
        if (serviceOfferingIds.isEmpty()) {
            return List.of();
        }

        ScheduleClient.ServiceScheduleListResponse response = scheduleClient
                .findSchedules(serviceOfferingIds, startDate)
                .data();

        if (response == null || response.content() == null) {
            throw new BusinessException(ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        return response.content().stream()
                .map(schedule -> new ScheduleSlot(
                        schedule.serviceOfferingId(),
                        schedule.date(),
                        schedule.startedAt().toLocalTime(),
                        schedule.finishedAt().toLocalTime()
                ))
                .toList();
    }
}