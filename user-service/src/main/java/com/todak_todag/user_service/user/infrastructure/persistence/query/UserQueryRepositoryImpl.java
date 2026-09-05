package com.todak_todag.user_service.user.infrastructure.persistence.query;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaUserRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserQueryRepositoryImpl implements UserQueryRepository {

	private final JpaUserRepository jpaRepo;

	@Override
	public Optional<User> findById(UUID userId) {
		return jpaRepo.findById(userId);
	}

	@Override
	public Optional<User> findLoginByUsername(String username) {
		return jpaRepo.findByUsernameAndStatusIn(username, List.of(UserStatus.APPROVED, UserStatus.WITHDRAWN, UserStatus.PENDING));
	}

	@Override
	public Optional<User> findActiveById(UUID userId) {
		return jpaRepo.findByIdAndStatus(userId, UserStatus.APPROVED);
	}

	@Override
	public boolean duplicateUsername(String username) {
		return jpaRepo.existsByUsername(username);
	}
	
	@Override
	public Optional<User> findAdminById(UUID userId) {
		return jpaRepo.findByIdAndRole(userId, UserRole.ADMIN);
	}

	@Override
	public Set<UUID> findMatchableSocialWorkerIds(UUID regionId) {
		return jpaRepo.findByRegionIdAndRoleAndStatus(regionId, UserRole.SOCIAL_WORKER, UserStatus.APPROVED);
	}
}
