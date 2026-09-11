package com.todak_todag.social_worker_service.matching.infrastructure.client;

import java.util.Set;
import java.util.UUID;

public record UserMatchableSocialWorkersResponse(
        Set<UUID> socialWorkerIds
) {
}