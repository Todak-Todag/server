package com.todak_todag.schedule_service.global.config;

import com.todak_todag.schedule_service.global.security.HeaderAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@EnableMethodSecurity(proxyTargetClass = true)
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Gateway Header -> UserContext 로 파싱
                // Controller 에서는 @AuthenticationPrincipal UserContext user 로 사용가능합니다.
                // ROLE 접두사가 붙습니다.
                // @PreAuthorize("hasRole('MASTER')") 로 Controller 에서 사용할 수 있습니다.
                // 여러가지의 경우 @PreAuthorize("hasAnyRole('MASTER', 'ADMIN')") 으로 사용할 수 있습니다.
                .addFilterBefore(new HeaderAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // 공개 API
                        .requestMatchers(
                                // TODO: 개발 TEST를 위해 임시 작성하였으며, 9/9일 이후 변경 예정
                                "/api/v1/service-results/**",
                                "/api/v1/service-schedules/**",
                                "/internal/v1/**",
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
