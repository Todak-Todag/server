package com.todak_todag.discharge_service.global.config;

import com.todak_todag.discharge_service.global.security.InternalApiKeyInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final InternalApiKeyInterceptor internalApiKeyInterceptor;

    @Override
    public void addInterceptors(
            InterceptorRegistry registry
    ) {
        registry
                .addInterceptor(
                        internalApiKeyInterceptor
                )
                .addPathPatterns(
                        "/internal/v1/**"
                )
                .order(0);
    }
}