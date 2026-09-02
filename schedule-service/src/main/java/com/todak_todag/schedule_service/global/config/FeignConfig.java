package com.todak_todag.schedule_service.global.config;

import feign.Logger;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// 내부 API 호출(Feign) 시 X-Internal-Api-Key 헤더를 자동으로 실어 보냄
@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor internalApiInterceptor(@Value("${internal.key}") String internalKey) {
        return requestTemplate -> requestTemplate.header("X-Internal-Api-Key", internalKey);
    }

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.BASIC;
    }
}
