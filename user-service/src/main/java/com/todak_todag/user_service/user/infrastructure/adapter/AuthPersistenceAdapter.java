package com.todak_todag.user_service.user.infrastructure.adapter;

import org.springframework.stereotype.Repository;

import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.repository.command.AuthCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.AuthQueryRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaAuthRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuthPersistenceAdapter implements AuthCommandRepository, AuthQueryRepository {

	private final JpaAuthRepository jpaRepository;

	@Override
	public Auth save(Auth auth) {
		return jpaRepository.save(auth);
	}
	
	
}
