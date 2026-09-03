package com.todak_todag.user_service.user.infrastructure.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.todak_todag.user_service.user.application.port.TokenStoragePort;
import com.todak_todag.user_service.user.domain.entity.auth.PhantomToken;
import com.todak_todag.user_service.user.infrastructure.persistence.RedisTokenRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisTokenStorageAdapter implements TokenStoragePort {
	
	private final RedisTokenRepository tokenRepo;
	
	@Override
	public void saveToken(PhantomToken phantomToken) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Optional<PhantomToken> findAccessTokenByHash(String phantomToken) {
		// TODO Auto-generated method stub
		return Optional.empty();
	}

	@Override
	public void deleteToken(String phantomToken) {
		// TODO Auto-generated method stub
		
	}

}
