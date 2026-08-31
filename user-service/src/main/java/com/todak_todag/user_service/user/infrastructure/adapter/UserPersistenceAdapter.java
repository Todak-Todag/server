package com.todak_todag.user_service.user.infrastructure.adapter;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.todak_todag.user_service.user.domain.entity.User;
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
		return null;
	}

	@Override
	public Optional<User> findById(UUID userId) {
		return Optional.empty();
	}
	
}
