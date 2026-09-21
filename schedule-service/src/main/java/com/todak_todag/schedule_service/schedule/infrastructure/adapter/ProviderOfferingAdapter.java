package com.todak_todag.schedule_service.schedule.infrastructure.adapter;

import com.todak_todag.schedule_service.schedule.application.port.ProviderOfferingPort;
import com.todak_todag.schedule_service.schedule.infrastructure.client.InternalApiResponses;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.ProviderServiceOfferingInternalResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.dto.ServiceOfferingIdListInternalResponse;
import com.todak_todag.schedule_service.schedule.infrastructure.client.provider.ProviderServiceOfferingClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderOfferingAdapter implements ProviderOfferingPort {

    private final ProviderServiceOfferingClient providerServiceOfferingClient;

    // 호출 실패(non-2xx/연결 실패)는 InternalApiErrorDecoder와 GlobalExceptionHandler가 변환
    // 여기서는 2xx인데 본문이 비어 오는 계약 위반만 걸러 NPE(=500)가 되지 않게 함
    @Override
    public UUID findAssignedProviderId(UUID serviceOfferingId) {
        ProviderServiceOfferingInternalResponse response = InternalApiResponses.requireData(
                providerServiceOfferingClient.findServiceOffering(serviceOfferingId), "provider-service.findServiceOffering");

        return InternalApiResponses.require(response.providerId(), "provider-service.findServiceOffering.providerId");
    }

    @Override
    public List<UUID> findServiceOfferingIds(UUID providerId) {
        ServiceOfferingIdListInternalResponse response = InternalApiResponses.requireData(
                providerServiceOfferingClient.findServiceOfferingIds(providerId), "provider-service.findServiceOfferingIds");

        return InternalApiResponses.require(response.content(), "provider-service.findServiceOfferingIds.content");
    }
}
