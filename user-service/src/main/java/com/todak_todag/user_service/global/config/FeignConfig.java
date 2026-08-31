package com.todak_todag.user_service.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import feign.Logger;
import feign.RequestInterceptor;

@Configuration
public class FeignConfig {

	@Bean
	public RequestInterceptor internalApiInterceptor(@Value("${internal.api-key}") String internalApiKey) {
		return requestTemplate -> requestTemplate.header("X-Internal-Api-Key", internalApiKey);
	}
	
	@Bean
	public Logger.Level feignLoggerLevel() {
		return Logger.Level.BASIC;
	}
	
}
