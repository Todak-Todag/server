package com.todak_todag.schedule_service.global.config;

import com.todak_todag.schedule_service.global.security.GatewayAuthenticationConverter;
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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Spring Security OAuth2 Resource Server
                // bearerTokenResolver 의 원래 표준은 Authorization: Bearer <토큰> 헤더인데
                // X-Gateway-Token 이라는 게이트웨이가 전달하는 별도의 헤더에 토큰을 넣으므로 그 헤더 이름을 지정한다.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(new HeaderBearerTokenResolver("X-Gateway-Token"))

                        // 찾은 토큰을 어떻게 검증하고 검증되면 무엇으로 바꾸는가? -> .jwt(...)
                        // SpringContext 안의 JwtDecoder Bean (바로아래 새로 정의한 gatewayTokenDecoder Bean)을 찾아서 사용
                        // GatewayAuthenticationConverter 는 UserContext로 바꾸는 역할
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(new GatewayAuthenticationConverter()))
                )

                .authorizeHttpRequests(auth -> auth
                        // 내부 API 인가는 InternalApiKeyInterceptor가 담당
                        .requestMatchers("/internal/**").permitAll()

                        // 모니터링·문서
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
            @Value("${internal-jwt.jwk-set-uri}") String jwkSetUri,
            @Value("${internal-jwt.issuer}") String issuer,
            @Value("${internal-jwt.audience}") String audience,
            @Value("${internal-jwt.clock-skew}") Duration clockSkew
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri)

                // 알고리즘 고정시키기
                .jwsAlgorithm(SignatureAlgorithm.RS256)
                .build();

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<Jwt>(

                // exp 를 확인하되 설정한 초만큼 오차를 허용함.
                new JwtTimestampValidator(clockSkew),

                // iss 클레임이 게이트웨이가 발급한 것인지 확인
                new JwtIssuerValidator(issuer),

                // aud 클레임에 자신의 서비스 이름이 포함되어 있는가를 확인
                new JwtClaimValidator<List<String>>("aud", aud -> aud.contains(audience))
        ));

        return decoder;
    }
}
