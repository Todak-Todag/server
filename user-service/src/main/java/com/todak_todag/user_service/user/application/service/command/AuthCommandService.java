package com.todak_todag.user_service.user.application.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.user.application.port.TokenStoragePort;
import com.todak_todag.user_service.user.domain.repository.command.AuthCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.AuthQueryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class AuthCommandService {

	private final TokenStoragePort tokenStoragePort;
	
	private final AuthCommandRepository authCommandRepo;
	
	private final AuthQueryRepository authQueryRepo;
	
	
}
