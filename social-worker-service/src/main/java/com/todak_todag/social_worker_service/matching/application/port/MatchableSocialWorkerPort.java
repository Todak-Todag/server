package com.todak_todag.social_worker_service.matching.application.port;

import java.util.Set;
import java.util.UUID;

public interface MatchableSocialWorkerPort {

    Set<UUID> findMatchableSocialWorkerIds(
            UUID patientId
    );
}