package com.todak_todag.api_gateway.authentication;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import com.todak_todag.api_gateway.config.AuthenticationProperties;
import com.todak_todag.api_gateway.config.RefreshTokenProperties;
import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.exception.TokenException;

import reactor.test.StepVerifier;

@DisplayName("ClientCookieConverter")
class ClientCookieConverterTest {

    private static final String ACCESS_COOKIE = "AccessToken";

    private static final String REFRESH_COOKIE = "RefreshToken";

    private final ClientCookieConverter converter = new ClientCookieConverter(
            new AuthenticationProperties(ACCESS_COOKIE, "access:", "sub", "role"),
            new RefreshTokenProperties(REFRESH_COOKIE)
    );

    @Test
    @DisplayName("AccessToken 쿠키가 있으면 인증 시도 객체를 반환한다")
    void returnsUnauthenticatedTokenWhenAccessCookiePresent() {
        ServerWebExchange exchange = exchangeWithCookies(
                new HttpCookie(ACCESS_COOKIE, "token-value")
        );

        StepVerifier.create(converter.convert(exchange))
                .assertNext(authentication -> assertThat(authentication.getCredentials())
                        .isEqualTo("token-value"))
                .verifyComplete();
    }

    @Test
    @DisplayName("AccessToken, RefreshToken 쿠키 둘 다 없으면 인증 시도 자체를 하지 않는다")
    void returnsEmptyWhenNoCookiesAtAll() {
        ServerWebExchange exchange = exchangeWithCookies();

        StepVerifier.create(converter.convert(exchange))
                .verifyComplete();
    }

    @Test
    @DisplayName("AccessToken 쿠키는 없고 RefreshToken 쿠키만 있으면 EXPIRED_ACCESS_TOKEN 이다")
    void failsWithExpiredAccessTokenWhenOnlyRefreshCookiePresent() {
        ServerWebExchange exchange = exchangeWithCookies(
                new HttpCookie(REFRESH_COOKIE, "refresh-value")
        );

        StepVerifier.create(converter.convert(exchange))
                .expectErrorSatisfies(tokenErrorOf(TokenErrorCode.EXPIRED_ACCESS_TOKEN))
                .verify();
    }

    @Test
    @DisplayName("RefreshToken 쿠키 값이 blank 면 없는 것과 동일하게 취급한다")
    void treatsBlankRefreshCookieAsAbsent() {
        ServerWebExchange exchange = exchangeWithCookies(
                new HttpCookie(REFRESH_COOKIE, "")
        );

        StepVerifier.create(converter.convert(exchange))
                .verifyComplete();
    }

    @Test
    @DisplayName("AccessToken 쿠키가 여러 개면 RefreshToken 유무와 무관하게 INVALID_ACCESS_TOKEN 이다")
    void failsWithInvalidWhenMultipleAccessCookies() {
        ServerWebExchange exchange = exchangeWithCookies(
                new HttpCookie(ACCESS_COOKIE, "a"),
                new HttpCookie(ACCESS_COOKIE, "b"),
                new HttpCookie(REFRESH_COOKIE, "refresh-value")
        );

        StepVerifier.create(converter.convert(exchange))
                .expectErrorSatisfies(tokenErrorOf(TokenErrorCode.INVALID_ACCESS_TOKEN))
                .verify();
    }

    private static ServerWebExchange exchangeWithCookies(HttpCookie... cookies) {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.get("/api/v1/users/1");

        for (HttpCookie cookie : cookies) {
            builder = builder.cookie(cookie);
        }

        return MockServerWebExchange.from(builder.build());
    }

    private static Consumer<Throwable> tokenErrorOf(TokenErrorCode expected) {
        return error -> {
            assertThat(error).isInstanceOf(TokenException.class);
            assertThat(((TokenException) error).getErrorCode()).isEqualTo(expected);
        };
    }

}