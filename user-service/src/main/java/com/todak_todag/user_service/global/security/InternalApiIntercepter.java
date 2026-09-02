package com.todak_todag.user_service.global.security;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InternalApiIntercepter implements HandlerInterceptor {

	private static final String INTERNAL_HEADER = "X-Internal-Api-Key";
	
	
}
