package com.todak_todag.user_service.user.infrastructure.persistence;

import org.springframework.data.repository.CrudRepository;

import com.todak_todag.user_service.user.domain.entity.auth.PhantomToken;

public interface RedisTokenRepository extends CrudRepository<PhantomToken, String> {

}
