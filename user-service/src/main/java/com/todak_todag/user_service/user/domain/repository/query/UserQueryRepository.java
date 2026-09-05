package com.todak_todag.user_service.user.domain.repository.query;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.todak_todag.user_service.user.domain.entity.user.User;

public interface UserQueryRepository {

	Optional<User> findById(UUID userId);
	
	Optional<User> findLoginByUsername(String username);

	Optional<User> findActiveById(UUID userId);
	
	Optional<User> findAdminById(UUID userId);
	
	boolean duplicateUsername(String username);

	Set<UUID> findMatchableSocialWorkerIds(UUID regionId);
}
