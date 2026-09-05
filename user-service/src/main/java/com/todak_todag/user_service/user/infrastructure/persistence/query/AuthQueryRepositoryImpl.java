package com.todak_todag.user_service.user.infrastructure.persistence.query;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.repository.query.AuthQueryRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaAuthRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuthQueryRepositoryImpl implements AuthQueryRepository {

	private final JpaAuthRepository jpaRepo;

	@Override
	public Optional<Auth> findActiveByUserId(UUID userId) {
		return jpaRepo.findByUserIdAndLogoutAtIsNull(userId);
	}
	
	
}
