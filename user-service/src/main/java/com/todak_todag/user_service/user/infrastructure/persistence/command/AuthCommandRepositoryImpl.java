package com.todak_todag.user_service.user.infrastructure.persistence.command;

import org.springframework.stereotype.Repository;

import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.repository.command.AuthCommandRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaAuthRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AuthCommandRepositoryImpl implements AuthCommandRepository {

	private final JpaAuthRepository jpaRepo;

	@Override
	public Auth save(Auth auth) {
		return jpaRepo.save(auth);
	}
	
}
