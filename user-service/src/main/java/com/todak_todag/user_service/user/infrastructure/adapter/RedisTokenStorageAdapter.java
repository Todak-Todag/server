package com.todak_todag.user_service.user.infrastructure.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.application.port.TokenStoragePort;
import com.todak_todag.user_service.user.domain.entity.auth.AccessToken;
import com.todak_todag.user_service.user.infrastructure.persistence.RedisTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisTokenStorageAdapter implements TokenStoragePort {
	
	private final RedisTokenRepository tokenRepo;
	
	private final TokenPort tokenPort;
	
	@Override
	public void saveToken(AccessToken accessToken) {
		tokenRepo.save(accessToken);
	}

	@Override
	public Optional<AccessToken> findAccessTokenByHash(String accessToken) {
		if(accessToken == null || accessToken.isBlank()) {
			return Optional.empty();
		}
		
		String phantomTokenHash = tokenPort.hashAccessToken(accessToken);
		
		return tokenRepo.findById(phantomTokenHash);
	}

	@Override
	public void deleteToken(String accessToken) {
		String phantomTokenHash = tokenPort.hashAccessToken(accessToken);
		
		tokenRepo.deleteById(phantomTokenHash);
	}

}
