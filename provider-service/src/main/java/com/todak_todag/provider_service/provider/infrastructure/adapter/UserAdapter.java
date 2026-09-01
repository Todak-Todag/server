package com.todak_todag.provider_service.provider.infrastructure.adapter;

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
        return userClient.findById(userId).regionId();
    }
}