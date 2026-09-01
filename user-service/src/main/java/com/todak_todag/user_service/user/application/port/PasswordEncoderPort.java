package com.todak_todag.user_service.user.application.port;

public interface PasswordEncoderPort {

	String encode(String password);
	
	boolean matches(String rawPassword, String encodedPassword);
}
