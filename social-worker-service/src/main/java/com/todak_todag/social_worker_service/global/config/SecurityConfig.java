
package com.todak_todag.social_worker_service.global.config;

import com.todak_todag.social_worker_service.global.security.GatewayAuthenticationConverter;

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
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.HeaderBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

import java.time.Duration;
import java.util.List;

@EnableWebSecurity
@EnableMethodSecurity
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                .sessionManagement(sess ->
                        sess.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Gateway가 발급한 내부 JWT 검증
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(
                                new HeaderBearerTokenResolver(
                                        "X-Gateway-Token"
                                )
                        )
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        new GatewayAuthenticationConverter()
                                )
                        )
                )

                .authorizeHttpRequests(auth -> auth

                        // 내부 API는 별도 API Key 검증 필요
                        .requestMatchers(
                                "/internal/v1/**"
                        ).permitAll()

                        // 모니터링 및 API 문서
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

                        // 나머지는 인증 필수
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public JwtDecoder gatewayTokenDecoder(

            @Value("${internal-jwt.jwk-set-uri}")
            String jwkSetUri,

            @Value("${internal-jwt.issuer}")
            String issuer,

            @Value("${internal-jwt.audience}")
            String audience,

            @Value("${internal-jwt.clock-skew}")
            Duration clockSkew
    ) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(jwkSetUri)
                        .jwsAlgorithm(SignatureAlgorithm.RS256)
                        .build();

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<Jwt>(

                        // 만료시간 검증
                        new JwtTimestampValidator(clockSkew),

                        // 발급자 검증
                        new JwtIssuerValidator(issuer),

                        // 서비스별 Audience 검증
                        new JwtClaimValidator<List<String>>(
                                "aud",
                                aud -> aud != null
                                        && aud.contains(audience)
                        )
                )
        );

        return decoder;
    }
}
