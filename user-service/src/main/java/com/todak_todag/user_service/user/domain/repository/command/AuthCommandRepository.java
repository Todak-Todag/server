package com.todak_todag.user_service.user.domain.repository.command;

import com.todak_todag.user_service.user.domain.entity.auth.Auth;

public interface AuthCommandRepository {

	Auth save(Auth auth);
}
