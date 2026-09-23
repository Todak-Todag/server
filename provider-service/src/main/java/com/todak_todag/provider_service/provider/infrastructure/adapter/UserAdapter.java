package com.todak_todag.provider_service.provider.infrastructure.adapter;

import com.todak_todag.provider_service.global.exception.BusinessException;
import com.todak_todag.provider_service.global.exception.ProviderErrorCode;
import com.todak_todag.provider_service.provider.application.port.UserPort;
import com.todak_todag.provider_service.provider.infrastructure.client.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserAdapter implements UserPort {

    private final UserClient userClient;

    @Override
    public UUID findRegionIdByUserId(UUID userId) {
        UserClient.UserInternalResponse response = userClient.findById(userId).data();

        if (response == null) {
            throw new BusinessException(ProviderErrorCode.EXTERNAL_SERVICE_UNAVAILABLE);
        }

        return response.regionId();
    }
}