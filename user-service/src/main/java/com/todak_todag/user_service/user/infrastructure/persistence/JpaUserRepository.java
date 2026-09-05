package com.todak_todag.user_service.user.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;

public interface JpaUserRepository extends JpaRepository<User, UUID> {

	boolean existsByUsername(String username);

	Optional<User> findByUsernameAndStatusIn(String username, List<UserStatus> status);
	
	Optional<User> findByIdAndStatus(UUID id, UserStatus status);
	
	Optional<User> findByIdAndRole(UUID id, UserRole role);

	@Query("""
			SELECT u.id FROM User u
			WHERE u.regionId =:regionId
					AND u.role =:role
					AND u.status =:status
	""")
	Set<UUID> findByRegionIdAndRoleAndStatus(UUID regionId, UserRole socialWorker, UserStatus approved);
}
