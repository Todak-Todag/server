package com.todak_todag.user_service.user.application.port;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.todak_todag.user_service.user.application.query.UserSearchQuery;
import com.todak_todag.user_service.user.application.result.UserSearchResult;

public interface UserSearchPort {
	Page<UserSearchResult> search(UserSearchQuery query, Pageable pageable);
}
