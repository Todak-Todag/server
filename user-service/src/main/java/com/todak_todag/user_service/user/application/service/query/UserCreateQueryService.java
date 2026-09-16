package com.todak_todag.user_service.user.application.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.todak_todag.user_service.user.application.support.AddressValidator;
import com.todak_todag.user_service.user.application.support.ConsentDocumentValidator;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCreateQueryService {

	private final ConsentDocumentValidator consentDocumentValidator;
  
	private final AddressValidator addressValidator;
  
	private final UserQueryRepository userQueryRepo;
  
	private final RegionQueryRepository regionQueryRepo;
	
	
}
