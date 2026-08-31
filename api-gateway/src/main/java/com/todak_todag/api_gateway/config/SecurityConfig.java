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
						
						// 공개 API
						.pathMatchers(
								"/api/v1/regions/**",
								"/api/v1/consent-documents/**"
						).permitAll()
						
						// 관리자
						.pathMatchers(
								"/api/v1/admin/**"
						).authenticated()
								
								// User-Service
								.pathMatchers(
										"/api/v1/users/**",
										"/api/v1/consents/**"
								).authenticated()
								
								// Discharge-Service
								.pathMatchers(
										"/api/v1/discharges/**"
								).authenticated()
								
								// Social-Worker-Service
								.pathMatchers(
										"/api/v1/social-worker-matchings/**"
								).authenticated()
								
								// Schedule-Service
								.pathMatchers(
										"/api/v1/service-schedules/**",
										"/api/v1/service-results/**"
								).authenticated()
								
								// Provider-Service
								.pathMatchers(
										"/api/v1/provide-services/**",
										"/api/v1/service-offerings/**",
										"/api/v1/provide-works/**"
								).authenticated()
								
								// Care-Plan-Service
								.pathMatchers(
										"/api/v1/care-plans/**",
										"/api/v1/care-plan-services/**",
										"/api/v1/service-preferences/**"
								).authenticated()
								
						.anyExchange().authenticated()
				);
		
		return http.build();
	}
	
}
