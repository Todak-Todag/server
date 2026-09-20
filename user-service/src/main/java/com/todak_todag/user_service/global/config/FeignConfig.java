package com.todak_todag.user_service.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.todak_todag.user_service.global.security.ServiceTokenClient;

import feign.Logger;
import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class FeignConfig {

//	@Bean
//	public RequestInterceptor internalApiInterceptor(@Value("${internal.key}") String internalKey) {
//		if(internalKey == null || internalKey.isBlank()) {
//			throw new IllegalArgumentException("[User] 서버 구동 실패 FeignConfig:internal.key 설정 오류");
//		}
//		
//		return requestTemplate -> requestTemplate.header(InternalHeader.INTERNAL_KEY, internalKey);
//	}
	
	@Bean
	public RequestInterceptor internalApiInterceptor(
			ServiceTokenClient tokenClient,
			@Value("${internal.key}") String internalKey
	) {
		return template -> {
			String audience = template.feignTarget().name();
			template.header("Internal-Service-Token", tokenClient.getToken(audience));
			
			// 기존 레거시 인증 - 마이크로서비스 전환 완료 후 삭제
			template.header("X-Internal-Api-Key", internalKey);
		};
	}
	
	@Bean
	public Logger.Level feignLoggerLevel() {
		return Logger.Level.BASIC;
	}
	
}
