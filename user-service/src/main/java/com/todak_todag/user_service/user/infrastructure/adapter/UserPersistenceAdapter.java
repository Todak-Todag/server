package com.todak_todag.user_service.user.infrastructure.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;
import com.todak_todag.user_service.user.domain.repository.command.UserCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaUserRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserPersistenceAdapter implements UserCommandRepository , UserQueryRepository {

	private final JpaUserRepository jpaRepository;

	@Override
	public User save(User user) {
		return jpaRepository.save(user);
	}

	@Override
	public Optional<User> findById(UUID userId) {
		return jpaRepository.findById(userId);
	}

	@Override
	public boolean duplicateUsername(String username) {
		return jpaRepository.existsByUsername(username);
	}

	@Override
	public Optional<User> findLoginByUsername(String username) {
		return jpaRepository.findByUsernameAndStatusIn(username, List.of(UserStatus.APPROVED, UserStatus.WITHDRAWN));
	}
	
}
