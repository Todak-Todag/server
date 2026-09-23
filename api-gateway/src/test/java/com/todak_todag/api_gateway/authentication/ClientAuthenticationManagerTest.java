package com.todak_todag.api_gateway.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import com.todak_todag.api_gateway.exception.TokenErrorCode;
import com.todak_todag.api_gateway.exception.TokenException;
import com.todak_todag.api_gateway.store.AccessTokenStore;
import com.todak_todag.api_gateway.token.AccessTokenClaims;
import com.todak_todag.api_gateway.token.JwtTokenParser;
import com.todak_todag.api_gateway.token.TokenHashGenerator;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClientAuthenticationManager")
class ClientAuthenticationManagerTest {

	private static final String COOKIE_TOKEN = "test-access-token";

	/**
	 * COOKIE_TOKEN 의 SHA-256 (UTF-8, 소문자 hex).
	 * TokenHashGenerator 로 계산하면 순환 검증이 되므로 외부에서 구한 고정값을 사용한다.
	 */
	private static final String EXPECTED_TOKEN_HASH =
			"597480d4b62ca612193f19e73fe4cc3ad17f0bf9cfc16a7cbf4b5064131c4805";

	private static final String USER_ID = "1";

	private static final String ROLE = "USER";

	@Mock
	private AccessTokenStore accessTokenStore;

	@Mock
	private JwtTokenParser jwtTokenParser;

	private ClientAuthenticationManager clientAuthenticationManager;

	@BeforeEach
	void setUp() {
		// 해시 계산이 실제로 SHA-256 인지 확인해야 하므로 TokenHashGenerator 는 실제 구현을 쓴다.
		clientAuthenticationManager = new ClientAuthenticationManager(
				new TokenHashGenerator(),
				accessTokenStore,
				jwtTokenParser
		);
	}

	@Test
	@DisplayName("Cookie 토큰의 SHA-256 해시를 키로 저장소를 조회한다")
	void looksUpStoreWithSha256HashOfCookieToken() {
		when(accessTokenStore.findByHash(anyString())).thenReturn(Mono.just(COOKIE_TOKEN));
		when(jwtTokenParser.parse(COOKIE_TOKEN)).thenReturn(new AccessTokenClaims(USER_ID, ROLE));

		StepVerifier.create(clientAuthenticationManager.authenticate(unauthenticatedToken()))
				.expectNextCount(1)
				.verifyComplete();

		verify(accessTokenStore).findByHash(EXPECTED_TOKEN_HASH);
	}

	@Test
	@DisplayName("저장소에서 조회한 토큰의 JWT 가 정상이면 인증 완료 객체를 반환한다")
	void returnsAuthenticatedTokenWhenJwtIsValid() {
		when(accessTokenStore.findByHash(EXPECTED_TOKEN_HASH)).thenReturn(Mono.just(COOKIE_TOKEN));
		when(jwtTokenParser.parse(COOKIE_TOKEN)).thenReturn(new AccessTokenClaims(USER_ID, ROLE));

		StepVerifier.create(clientAuthenticationManager.authenticate(unauthenticatedToken()))
				.assertNext(authentication -> {
					assertThat(authentication).isInstanceOf(ClientAuthenticationToken.class);
					assertThat(authentication.isAuthenticated()).isTrue();
					assertThat(authentication.getPrincipal())
							.isEqualTo(new ClientContext(USER_ID, ROLE));
				})
				.verifyComplete();
	}

	@Test
	@DisplayName("인증 완료 객체는 role 앞에 ROLE_ 을 붙인 권한을 가진다")
	void grantsRolePrefixedAuthority() {
		when(accessTokenStore.findByHash(EXPECTED_TOKEN_HASH)).thenReturn(Mono.just(COOKIE_TOKEN));
		when(jwtTokenParser.parse(COOKIE_TOKEN)).thenReturn(new AccessTokenClaims(USER_ID, ROLE));

		StepVerifier.create(clientAuthenticationManager.authenticate(unauthenticatedToken()))
				.assertNext(authentication -> assertThat(authentication.getAuthorities())
						.extracting(GrantedAuthority::getAuthority)
						.containsExactly("ROLE_USER"))
				.verifyComplete();
	}

	@Test
	@DisplayName("저장소에 토큰이 없으면 INVALID_ACCESS_TOKEN 이다")
	void failsWithInvalidAccessTokenWhenStoreIsEmpty() {
		when(accessTokenStore.findByHash(EXPECTED_TOKEN_HASH)).thenReturn(Mono.empty());

		StepVerifier.create(clientAuthenticationManager.authenticate(unauthenticatedToken()))
				.expectErrorSatisfies(tokenErrorOf(TokenErrorCode.INVALID_ACCESS_TOKEN))
				.verify();

		verify(jwtTokenParser, never()).parse(anyString());
	}

	@Test
	@DisplayName("저장소 조회 중 DataAccessException 이 발생하면 AUTHENTICATION_SERVICE_UNAVAILABLE 이다")
	void mapsDataAccessExceptionToServiceUnavailable() {
		RedisConnectionFailureException redisFailure =
				new RedisConnectionFailureException("redis unavailable");

		when(accessTokenStore.findByHash(EXPECTED_TOKEN_HASH)).thenReturn(Mono.error(redisFailure));

		StepVerifier.create(clientAuthenticationManager.authenticate(unauthenticatedToken()))
				.expectErrorSatisfies(error -> {
					tokenErrorOf(TokenErrorCode.AUTHENTICATION_SERVICE_UNAVAILABLE).accept(error);
					assertThat(error).hasCause(redisFailure);
				})
				.verify();

		verify(jwtTokenParser, never()).parse(anyString());
	}

	@Test
	@DisplayName("JWT 파싱 실패는 해당 오류 코드 그대로 전파된다")
	void propagatesJwtParsingFailure() {
		when(accessTokenStore.findByHash(EXPECTED_TOKEN_HASH)).thenReturn(Mono.just(COOKIE_TOKEN));
		when(jwtTokenParser.parse(COOKIE_TOKEN))
				.thenThrow(new TokenException(TokenErrorCode.EXPIRED_ACCESS_TOKEN));

		StepVerifier.create(clientAuthenticationManager.authenticate(unauthenticatedToken()))
				.expectErrorSatisfies(tokenErrorOf(TokenErrorCode.EXPIRED_ACCESS_TOKEN))
				.verify();
	}

	@Test
	@DisplayName("인증에 성공하면 요청 토큰의 credentials 가 제거된다")
	void erasesCredentialsAfterSuccess() {
		when(accessTokenStore.findByHash(EXPECTED_TOKEN_HASH)).thenReturn(Mono.just(COOKIE_TOKEN));
		when(jwtTokenParser.parse(COOKIE_TOKEN)).thenReturn(new AccessTokenClaims(USER_ID, ROLE));

		ClientAuthenticationToken requestToken = unauthenticatedToken();

		StepVerifier.create(clientAuthenticationManager.authenticate(requestToken))
				.expectNextCount(1)
				.verifyComplete();

		assertThat(requestToken.getCredentials()).isNull();
	}

	@Test
	@DisplayName("인증에 실패해도 요청 토큰의 credentials 가 제거된다")
	void erasesCredentialsAfterFailure() {
		when(accessTokenStore.findByHash(EXPECTED_TOKEN_HASH)).thenReturn(Mono.empty());

		ClientAuthenticationToken requestToken = unauthenticatedToken();

		StepVerifier.create(clientAuthenticationManager.authenticate(requestToken))
				.expectErrorSatisfies(tokenErrorOf(TokenErrorCode.INVALID_ACCESS_TOKEN))
				.verify();

		assertThat(requestToken.getCredentials()).isNull();
	}

	@Test
	@DisplayName("ClientAuthenticationToken 이 아닌 인증 요청은 처리하지 않고 비워서 반환한다")
	void ignoresUnsupportedAuthenticationType() {
		Authentication unsupported = new UsernamePasswordAuthenticationToken("user", "password");

		StepVerifier.create(clientAuthenticationManager.authenticate(unsupported))
				.verifyComplete();

		verifyNoInteractions(accessTokenStore, jwtTokenParser);
	}

	@Test
	@DisplayName("credentials 가 이미 제거된 토큰은 저장소를 조회하지 않고 INVALID_ACCESS_TOKEN 이다")
	void failsWithInvalidAccessTokenWhenCredentialsAlreadyErased() {
		ClientAuthenticationToken requestToken = unauthenticatedToken();
		requestToken.eraseCredentials();

		StepVerifier.create(clientAuthenticationManager.authenticate(requestToken))
				.expectErrorSatisfies(tokenErrorOf(TokenErrorCode.INVALID_ACCESS_TOKEN))
				.verify();

		verifyNoInteractions(accessTokenStore, jwtTokenParser);
	}

	private ClientAuthenticationToken unauthenticatedToken() {
		return ClientAuthenticationToken.unauthenticated(COOKIE_TOKEN);
	}

	private static Consumer<Throwable> tokenErrorOf(TokenErrorCode expected) {
		return error -> {
			assertThat(error).isInstanceOf(TokenException.class);
			assertThat(((TokenException) error).getErrorCode()).isEqualTo(expected);
		};
	}

}
