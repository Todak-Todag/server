package com.todak_todag.user_service.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.todak_todag.user_service.global.security.HeaderAuthenticationFilter;

@EnableWebSecurity
@EnableMethodSecurity
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
								"/api/v1/auth/login",
								"/api/v1/auth/signup",
								"/api/v1/auth/reissue",
								"/api/v1/auth/logout",
								"/api/v1/regions/**"
						).permitAll()
						
						.anyRequest().authenticated()
				)
				;
		
		return http.build();
	}
}
