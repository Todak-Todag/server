package com.todak_todag.api_gateway.config;

import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

	// 인증이 쿠키 기반이라 allow-credentials 가 켜져 있다. 이 경우 와일드카드 Origin 은 쓸 수 없으므로
	// 허용 Origin 은 cors.allowed-origins 로 명시한다.
	private static final Duration MAX_AGE = Duration.ofHours(1);

	@Bean
	public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
		CorsConfiguration configuration = new CorsConfiguration();

		configuration.setAllowedOrigins(corsProperties.allowedOrigins());
		configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(MAX_AGE);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
