package com.todak_todag.user_service.user.infrastructure.adapter;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PasswordEncoderAdapter implements PasswordEncoderPort {

	private final PasswordEncoder passwordEncoder;

	@Override
	public String encode(String password) {
		return passwordEncoder.encode(password);
	}

	@Override
	public boolean matches(String rawPassword, String encodedPassword) {
		return passwordEncoder.matches(rawPassword, encodedPassword);
	}
	
	
}
