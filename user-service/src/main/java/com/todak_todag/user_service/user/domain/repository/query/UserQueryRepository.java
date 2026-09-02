package com.todak_todag.user_service.user.domain.repository.query;

import java.util.Optional;
import java.util.UUID;

import com.todak_todag.user_service.user.domain.entity.user.User;

public interface UserQueryRepository {

	Optional<User> findById(UUID userId);
	
	boolean duplicateUsername(String username);
}
