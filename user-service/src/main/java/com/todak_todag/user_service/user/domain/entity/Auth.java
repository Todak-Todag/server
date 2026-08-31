package com.todak_todag.user_service.user.domain.entity;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Auth {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "auth_id")
	private UUID id;
}
