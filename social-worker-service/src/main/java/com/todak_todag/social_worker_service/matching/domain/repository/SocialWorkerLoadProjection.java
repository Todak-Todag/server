package com.todak_todag.social_worker_service.matching.domain.repository;

import java.util.UUID;

public interface SocialWorkerLoadProjection {

    UUID getSocialWorkerId();

    Long getActiveCount();
}