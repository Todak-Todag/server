package com.todak_todag.user_service.user.domain.repository.query;

import java.util.Optional;
import java.util.UUID;

import com.todak_todag.user_service.user.domain.entity.auth.Auth;

public interface AuthQueryRepository {

	Optional<Auth> findActiveByUserId(UUID userId);
}
