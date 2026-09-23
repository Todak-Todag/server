package com.todak_todag.user_service.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.todak_todag.user_service.global.security.InternalResponseInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final InternalResponseInterceptor internalApiInterceptor;
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry
				.addInterceptor(internalApiInterceptor)
				.addPathPatterns("/internal/**")
				.order(0);
	}
	
}
