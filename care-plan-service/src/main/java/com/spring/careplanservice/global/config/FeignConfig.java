package com.spring.careplanservice.global.config;

import com.spring.careplanservice.global.security.InternalHeader;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor internalApiInterceptor(
            @Value("${internal.key}") String internalKey
    ) {
        return requestTemplate ->
                requestTemplate.header(
                        InternalHeader.INTERNAL_KEY,
                        internalKey
                );
    }
}
