package com.todak_todag.user_service.user.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.todak_todag.user_service.user.domain.entity.user.User;

public interface JpaUserRepository extends JpaRepository<User, UUID> {

}
