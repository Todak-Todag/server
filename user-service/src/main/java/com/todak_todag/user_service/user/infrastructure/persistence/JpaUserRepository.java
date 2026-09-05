package com.todak_todag.user_service.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;

public interface JpaUserRepository extends JpaRepository<User, UUID> {

	boolean existsByUsername(String username);

	Optional<User> findByUsernameAndStatusIn(String username, List<UserStatus> status);
	
	Optional<User> findByIdAndStatus(UUID id, UserStatus status);
	
	Optional<User> findByIdAndRole(UUID id, UserRole role);
}
