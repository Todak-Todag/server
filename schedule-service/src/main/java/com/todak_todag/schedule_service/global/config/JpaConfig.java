package com.todak_todag.schedule_service.global.config;

import com.todak_todag.schedule_service.global.security.SpringSecurityAuditorAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.UUID;

// SpringSecurityAuditorAware가 SecurityContext 기반으로 created_by/updated_by를 채움
// 인증 정보가 없으면 SystemId.SYSTEM_USER_ID로 대체
@Configuration
@EnableJpaAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class JpaConfig {

    @Bean
    public AuditorAware<UUID> springSecurityAuditorAware() {
        return new SpringSecurityAuditorAware();
    }
}
