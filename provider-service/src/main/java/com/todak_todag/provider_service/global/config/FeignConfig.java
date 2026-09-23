package com.todak_todag.provider_service.global.config;

import com.todak_todag.provider_service.global.security.InternalHeader;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    private static final String INTERNAL_KEY_HEADER = "X-Internal-Api-Key";

    @Bean
    public RequestInterceptor internalApiInterceptor(@Value("${internal.key}") String internalKey) {
        return requestTemplate -> requestTemplate.header(InternalHeader.INTERNAL_KEY, internalKey);
    }
}