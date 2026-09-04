package com.todak_todag.user_service.user.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "p_auths")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Auth {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "auth_id")
	private UUID id;
	
	@Column(name = "user_id", nullable = false)
	private UUID userId;
	
	@Column(name = "refresh_token_hash", nullable = false)
	private String refreshTokenHash;
	
	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;
	
	@Column(name = "login_at", nullable = false)
	private LocalDateTime loginAt;
	
	@Column(name = "logout_at")
	private LocalDateTime logoutAt;
	
	public static Auth login(UUID userId, String refreshTokenHash, LocalDateTime expiresAt, LocalDateTime loginAt) {
		Auth auth = new Auth();
		
		auth.userId = userId;
		auth.refreshTokenHash = refreshTokenHash;
		auth.expiresAt = expiresAt;
		auth.loginAt = loginAt;
		auth.logoutAt = null;
		
		return auth;
	}
	
	public void logout() {
		if(this.logoutAt != null) return;
		
		this.logoutAt = LocalDateTime.now();
	}
}
