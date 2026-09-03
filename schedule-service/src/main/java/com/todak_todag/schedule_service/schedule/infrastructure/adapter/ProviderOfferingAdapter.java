package com.todak_todag.schedule_service.schedule.infrastructure.adapter;

import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.infrastructure.client.provider.ProviderServiceOfferingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderOfferingAdapter implements ProviderOfferingPort {

    private final ProviderServiceOfferingClient providerServiceOfferingClient;

    @Override
    public UUID findAssignedProviderId(UUID serviceOfferingId) {
        return providerServiceOfferingClient.findServiceOffering(serviceOfferingId).data().providerId();
    }
}
