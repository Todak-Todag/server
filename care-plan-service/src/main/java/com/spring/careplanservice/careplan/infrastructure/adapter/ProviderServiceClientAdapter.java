package com.spring.careplanservice.careplan.infrastructure.adapter;

import com.spring.careplanservice.careplan.application.port.ProviderServiceQueryPort;
import com.spring.careplanservice.careplan.application.result.ProvideServiceInfoResult;
import com.spring.careplanservice.careplan.infrastructure.client.ProviderServiceFeignClient;
import com.spring.careplanservice.careplan.infrastructure.client.ProviderServiceInternalResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProviderServiceClientAdapter implements ProviderServiceQueryPort {
    private final ProviderServiceFeignClient providerServiceFeignClient;

    @Override
    public List<ProvideServiceInfoResult> findAllByIds(List<UUID> provideServiceIds) {
        ProviderServiceInternalResponse providerServiceInternalResponse = providerServiceFeignClient.search(
                provideServiceIds
        );

        return providerServiceInternalResponse.data().content().stream()
                .map(content -> new ProvideServiceInfoResult(
                        content.provideServiceId(),
                        content.name(),
                        content.content()
                ))
                .toList();
    }
}
