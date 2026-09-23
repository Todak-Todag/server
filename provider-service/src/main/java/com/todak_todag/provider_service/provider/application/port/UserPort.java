package com.todak_todag.provider_service.provider.application.port;

import java.util.UUID;

public interface UserPort {

    UUID findRegionIdByUserId(UUID userId);
}