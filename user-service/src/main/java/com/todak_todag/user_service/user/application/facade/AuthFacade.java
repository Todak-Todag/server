package com.todak_todag.user_service.user.application.facade;

import org.springframework.stereotype.Component;

import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.application.port.TokenStorePort;
import com.todak_todag.user_service.user.application.service.command.AuthCommandService;
import com.todak_todag.user_service.user.application.service.query.AuthQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthFacade {

	private final AuthQueryService authQueryService;
  
	private final AuthCommandService authCommandService;
  
	private final PasswordEncoderPort passwordEncoder;
  
	private final TokenStorePort tokenStorePort;
  
	private final TokenPort tokenPort;
	
	
}
