package com.todak_todag.provider_service.provider.domain.repository.query;

import java.util.UUID;

public interface ProvideServiceQueryRepository {

    boolean existsById(UUID provideServiceId);
}