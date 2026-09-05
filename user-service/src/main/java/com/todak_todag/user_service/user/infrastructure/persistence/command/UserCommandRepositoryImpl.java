package com.todak_todag.user_service.user.infrastructure.persistence.command;

import org.springframework.stereotype.Repository;

import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.repository.command.UserCommandRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaUserRepository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserCommandRepositoryImpl implements UserCommandRepository {

	private final JpaUserRepository jpaRepo;

	@Override
	public User save(User user) {
		return jpaRepo.save(user);
	}
	
	
}
