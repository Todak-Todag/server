
package com.todak_todag.discharge_service.global.config;

import com.todak_todag.discharge_service.global.security.GatewayAuthenticationConverter;

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
@EnableMethodSecurity(proxyTargetClass = true)
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

                        .requestMatchers(
                                "/internal/v1/**"
                        ).permitAll()

                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()

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

                        new JwtTimestampValidator(clockSkew),

                        new JwtIssuerValidator(issuer),

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
