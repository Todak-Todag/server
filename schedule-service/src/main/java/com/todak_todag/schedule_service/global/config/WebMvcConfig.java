package com.todak_todag.schedule_service.global.config;

import com.todak_todag.schedule_service.global.security.InternalResponseInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// 내부 API(/internal/v1/**)에 X-Internal-Api-Key 검증 Interceptor를 등록
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final InternalResponseInterceptor internalResponseInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(internalResponseInterceptor)
                .addPathPatterns("/internal/v1/**")
                .order(0);
    }
}
