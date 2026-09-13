package com.todak_todag.user_service.global.config;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
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
                        // 내부 서비스 간 호출 인가는 InternalApiIntercepter 담당
                        .requestMatchers("/internal/**")
                        .permitAll()

                        // 공개 API
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api/v1/auth/login",
                                "/api/v1/users/signup",
                                "/api/v1/auth/reissue",
                                "/api/v1/regions/**",
                                "/api/v1/consent-documents/**",
                                "/actuator/health/**",
                                "/actuator/info",
                                "/actuator/prometheus"
                        ).permitAll()

                        .anyRequest().authenticated()
                )
        ;

        return http.build();
    }
    
    @Bean
    public JwtDecoder gatewayTokenDecoder(
    		@Value("${internal-jwt.jwk-set-uri}") String jwkSetUri,
    		@Value("${internal-jwt.issuer}") String issuer,
    		@Value("${internal-jwt.audience}") String audience,
    		@Value("${internal-jwt.clock-skew}") Duration clockSkew
    ) {
    	NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
    			.jwsAlgorithm(SignatureAlgorithm.RS256)
    			.build();
    	
    	decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(
    			new JwtTimestampValidator(clockSkew),
    			new JwtIssuerValidator(issuer),
    			new JwtClaimValidator<List<String>>("aud", aud -> aud != null && aud.contains(audience))
    	));
    	
    	return decoder;
    }
}
