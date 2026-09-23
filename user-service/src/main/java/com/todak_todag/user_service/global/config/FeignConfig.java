package com.todak_todag.user_service.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.todak_todag.user_service.global.security.InternalHeader;

import feign.Logger;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class FeignConfig {

	@Bean
	public RequestInterceptor internalApiInterceptor(@Value("${internal.key}") String internalKey) {
		if(internalKey == null || internalKey.isBlank()) {
			throw new IllegalArgumentException("[User] 서버 구동 실패 FeignConfig:internal.key 설정 오류");
		}
		
		return requestTemplate -> requestTemplate.header(InternalHeader.INTERNAL_KEY, internalKey);
	}
	
	@Bean
	public Logger.Level feignLoggerLevel() {
		return Logger.Level.BASIC;
	}
	
}
