package com.todak_todag.user_service.user.infrastructure.persistence.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.todak_todag.user_service.user.application.port.UserSearchPort;
import com.todak_todag.user_service.user.application.query.UserSearchQuery;
import com.todak_todag.user_service.user.application.result.UserSearchResult;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserSearchRepository implements UserSearchPort {

	private final JpaUserRepository jpaRepo;

	@Override
	public Page<UserSearchResult> search(UserSearchQuery query, Pageable pageable) {
		return jpaRepo.search(query, pageable);
	}
	
}
