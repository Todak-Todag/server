package com.todak_todag.api_gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ServerWebExchange;

import com.todak_todag.api_gateway.authentication.ClientAuthenticationToken;
import com.todak_todag.api_gateway.authentication.ClientContext;
import com.todak_todag.api_gateway.token.InternalTokenIssuer;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("PhantomAuthenticationFilter")
class PhantomAuthenticationFilterTest {

	private static final String USER_ID_HEADER = "X-User-Id";

	private static final String USER_ROLE_HEADER = "X-User-Role";

	private static final String GATEWAY_TOKEN_HEADER = "X-Gateway-Token";

	private final InternalTokenIssuer tokenIssuer = mock(InternalTokenIssuer.class);

	private final PhantomAuthenticationFilter phantomAuthenticationFilter =
			new PhantomAuthenticationFilter(tokenIssuer);

	private final RecordingGatewayFilterChain chain = new RecordingGatewayFilterChain();

	@BeforeEach
	void setUp() {
		given(tokenIssuer.issue(any(), anyString())).willReturn("stub-gateway-token");
	}

	@Test
	@DisplayName("인증된 요청에는 검증된 X-User-Id, X-User-Role 이 추가된다")
	void addsClientHeadersForAuthenticatedRequest() {
		ServerWebExchange exchange = exchangeWithPrincipal(
				MockServerHttpRequest.get("/api/v1/users/1").build(),
				authenticated("1", "USER")
		);

		StepVerifier.create(phantomAuthenticationFilter.filter(exchange, chain))
				.verifyComplete();

		assertThat(chain.firstHeader(USER_ID_HEADER)).isEqualTo("1");
		assertThat(chain.firstHeader(USER_ROLE_HEADER)).isEqualTo("USER");
	}

	@Test
	@DisplayName("인증된 요청에는 InternalTokenIssuer 가 발급한 X-Gateway-Token 이 추가된다")
	void addsGatewayTokenForAuthenticatedRequest() {
		given(tokenIssuer.issue(any(), anyString())).willReturn("signed-gateway-token");

		ServerWebExchange exchange = exchangeWithPrincipal(
				MockServerHttpRequest.get("/api/v1/users/1").build(),
				authenticated("1", "USER")
		);

		StepVerifier.create(phantomAuthenticationFilter.filter(exchange, chain))
				.verifyComplete();

		assertThat(chain.firstHeader(GATEWAY_TOKEN_HEADER)).isEqualTo("signed-gateway-token");
	}

	@Test
	@DisplayName("Route 정보가 있으면 그 Route id 를 aud 로 넘겨 토큰 발급을 요청한다")
	void resolvesAudienceFromMatchedRoute() {
		Route route = mock(Route.class);
		given(route.getId()).willReturn("schedule-service");

		ClientContext client = new ClientContext("1", "USER");

		ServerWebExchange exchange = exchangeWithPrincipal(
				MockServerHttpRequest.get("/api/v1/service-schedules/1").build(),
				authenticated("1", "USER")
		);
		exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

		StepVerifier.create(phantomAuthenticationFilter.filter(exchange, chain))
				.verifyComplete();

		verify(tokenIssuer).issue(client, "schedule-service");
	}

	@Test
	@DisplayName("Route 정보가 없으면 aud 를 \"unknown\" 으로 넘겨 토큰 발급을 요청한다")
	void resolvesUnknownAudienceWhenRouteAttributeIsAbsent() {
		ClientContext client = new ClientContext("1", "USER");

		ServerWebExchange exchange = exchangeWithPrincipal(
				MockServerHttpRequest.get("/api/v1/users/1").build(),
				authenticated("1", "USER")
		);

		StepVerifier.create(phantomAuthenticationFilter.filter(exchange, chain))
				.verifyComplete();

		verify(tokenIssuer).issue(client, "unknown");
	}

	@Test
	@DisplayName("인증된 요청이라도 downstream 체인은 정확히 한 번만 호출된다")
	void invokesDownstreamChainExactlyOnceForAuthenticatedRequest() {
		ServerWebExchange exchange = exchangeWithPrincipal(
				MockServerHttpRequest.get("/api/v1/users/1").build(),
				authenticated("1", "USER")
		);

		StepVerifier.create(phantomAuthenticationFilter.filter(exchange, chain))
				.verifyComplete();

		assertThat(chain.invocationCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("외부에서 보낸 위조 사용자 헤더는 검증된 값으로 교체된다")
	void replacesForgedClientHeadersWithVerifiedValues() {
		ServerWebExchange exchange = exchangeWithPrincipal(
				MockServerHttpRequest.get("/api/v1/users/1")
						.header(USER_ID_HEADER, "9999")
						.header(USER_ROLE_HEADER, "ADMIN")
						.header(GATEWAY_TOKEN_HEADER, "forged-token")
						.build(),
				authenticated("1", "USER")
		);

		StepVerifier.create(phantomAuthenticationFilter.filter(exchange, chain))
				.verifyComplete();

		assertThat(chain.firstHeaderValues(USER_ID_HEADER)).containsExactly("1");
		assertThat(chain.firstHeaderValues(USER_ROLE_HEADER)).containsExactly("USER");
		assertThat(chain.firstHeaderValues(GATEWAY_TOKEN_HEADER)).containsExactly("stub-gateway-token");
	}

	@Test
	@DisplayName("인증되지 않은 요청의 위조 사용자 헤더는 제거되고 downstream 으로 전달되지 않는다")
	void stripsForgedClientHeadersWhenPrincipalIsAbsent() {
		ServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/users/1")
						.header(USER_ID_HEADER, "9999")
						.header(USER_ROLE_HEADER, "ADMIN")
						.header(GATEWAY_TOKEN_HEADER, "forged-token")
						.build()
		);

		StepVerifier.create(phantomAuthenticationFilter.filter(exchange, chain))
				.verifyComplete();

		assertThat(chain.invocationCount()).isEqualTo(1);
		assertThat(chain.lastHeader(USER_ID_HEADER)).isNull();
		assertThat(chain.lastHeader(USER_ROLE_HEADER)).isNull();
		assertThat(chain.lastHeader(GATEWAY_TOKEN_HEADER)).isNull();
	}

	@Test
	@DisplayName("인증되지 않은 ClientAuthenticationToken 은 사용자 헤더를 만들지 않는다")
	void doesNotAddClientHeadersForUnauthenticatedToken() {
		ServerWebExchange exchange = exchangeWithPrincipal(
				MockServerHttpRequest.get("/api/v1/users/1")
						.header(USER_ID_HEADER, "9999")
						.build(),
				ClientAuthenticationToken.unauthenticated("access-token")
		);

		StepVerifier.create(phantomAuthenticationFilter.filter(exchange, chain))
				.verifyComplete();

		assertThat(chain.invocationCount()).isEqualTo(1);
		assertThat(chain.lastHeader(USER_ID_HEADER)).isNull();
		assertThat(chain.lastHeader(USER_ROLE_HEADER)).isNull();
	}

	@Test
	@DisplayName("익명 인증은 authenticated 상태여도 principal 이 ClientContext 가 아니므로 사용자 헤더를 만들지 않는다")
	void doesNotAddClientHeadersForAnonymousAuthentication() {
		Authentication anonymous = new AnonymousAuthenticationToken(
				"anonymous-key",
				"anonymousUser",
				List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))
		);

		ServerWebExchange exchange = exchangeWithPrincipal(
				MockServerHttpRequest.get("/api/v1/users/1")
						.header(USER_ID_HEADER, "9999")
						.build(),
				anonymous
		);

		assertThat(anonymous.isAuthenticated()).isTrue();

		StepVerifier.create(phantomAuthenticationFilter.filter(exchange, chain))
				.verifyComplete();

		assertThat(chain.invocationCount()).isEqualTo(1);
		assertThat(chain.lastHeader(USER_ID_HEADER)).isNull();
		assertThat(chain.lastHeader(USER_ROLE_HEADER)).isNull();
	}

	@Test
	@DisplayName("Gateway 전역 필터 순서는 0 이다")
	void runsWithOrderZero() {
		assertThat(phantomAuthenticationFilter.getOrder()).isZero();
	}

	private static Authentication authenticated(String userId, String role) {
		return ClientAuthenticationToken.authenticated(
				new ClientContext(userId, role),
				List.of(new SimpleGrantedAuthority("ROLE_" + role))
		);
	}

	private static ServerWebExchange exchangeWithPrincipal(
			MockServerHttpRequest request,
			Principal principal
	) {
		return MockServerWebExchange.from(request)
				.mutate()
				.principal(Mono.just(principal))
				.build();
	}

	private static final class RecordingGatewayFilterChain implements GatewayFilterChain {

		private final List<ServerWebExchange> invocations = new ArrayList<>();

		@Override
		public Mono<Void> filter(ServerWebExchange exchange) {
			invocations.add(exchange);

			return Mono.empty();
		}

		private int invocationCount() {
			return invocations.size();
		}

		private ServerHttpRequest requestAt(int index) {
			assertThat(invocations).hasSizeGreaterThan(index);

			return invocations.get(index).getRequest();
		}

		private String firstHeader(String name) {
			return requestAt(0).getHeaders().getFirst(name);
		}

		private List<String> firstHeaderValues(String name) {
			return requestAt(0).getHeaders().get(name);
		}

		private String lastHeader(String name) {
			return requestAt(invocations.size() - 1).getHeaders().getFirst(name);
		}

	}

}