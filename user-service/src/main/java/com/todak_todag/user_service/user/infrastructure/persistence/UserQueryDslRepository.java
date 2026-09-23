package com.todak_todag.user_service.user.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.todak_todag.user_service.user.application.query.UserSearchQuery;
import com.todak_todag.user_service.user.application.result.UserSearchResult;

public interface UserQueryDslRepository {

	Page<UserSearchResult> search(UserSearchQuery query, Pageable pageable); 
}
