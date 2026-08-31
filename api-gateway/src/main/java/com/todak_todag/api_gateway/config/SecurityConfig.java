package com.todak_todag.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		http
				.csrf(ServerHttpSecurity.CsrfSpec::disable)
				.httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
				.formLogin(ServerHttpSecurity.FormLoginSpec::disable)
				.logout(ServerHttpSecurity.LogoutSpec::disable)
				.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
				
				.authorizeExchange(exchange -> exchange
						// Internal
						.pathMatchers(
								"/internal/**"
						).denyAll()
						
						// Swagger
						.pathMatchers(
								"/swagger-ui.html",
								"/swagger-ui/**",
								"/webjars/**",
								"/v3/api-docs/**"
						).permitAll()
						
						// Actuator
						.pathMatchers(
								"/actuator/health"
						).permitAll()
						
						// 로그인, 회원가입, 토큰재발급, 로그아웃
						.pathMatchers(
								HttpMethod.POST,
								"/api/v1/auth/login",
								"/api/v1/auth/signup",
								"/api/v1/auth/reissue",
								"/api/v1/auth/logout"
						).permitAll()
						
						// TODO: User-Service

							// TODO: User-Service 미인증 경로
						
							// TODO: User-Service 인증 경로
						
						// TODO: Discharge-Service
						
							// TODO: Discharge-Service 미인증 경로
						
							// TODO: Discharge-Service 인증 경로

						// TODO: Social-Worker-Service
						
							// TODO: Social-Worker-Service 미인증 경로
						
							// TODO: Social-Worker-Service 인증 경로
						
						// TODO: Schedule-Service
						
							// TODO: Schedule-Service 미인증 경로
						
							// TODO: Schedule-Service 인증 경로
						
						// TODO: Provider-Service
						
							// TODO: Provider-Service 미인증 경로
						
							// TODO: Provider-Service 인증 경로
						
						// TODO: Care-Plan-Service
						
							// TODO: Care-Plan-Service 미인증 경로
						
							// TODO: Care-Plan-Service 인증 경로
				);
		
		return http.build();
	}
	
}
