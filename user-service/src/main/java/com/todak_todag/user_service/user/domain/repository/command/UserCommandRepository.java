package com.todak_todag.user_service.user.domain.repository.command;

import com.todak_todag.user_service.user.domain.entity.User;

public interface UserCommandRepository {

	User save(User user);
}
