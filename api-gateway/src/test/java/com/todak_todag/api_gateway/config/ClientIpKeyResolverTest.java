package com.todak_todag.api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.net.InetSocketAddress;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.test.StepVerifier;

@DisplayName("RateLimitConfig.clientIpKeyResolver")
class ClientIpKeyResolverTest {

	// IPv6 프리픽스(/64) 계산이 맞는지는 실제 JVM 으로 미리 검증한 값이다.
	// InetAddress.getByName("2001:db8:85a3:0:0:8a2e:370:7334") 의 앞 8바이트 -> HexFormat
	private static final String SUBNET_A_HOST_1 = "2001:db8:85a3:0:0:8a2e:370:7334";

	private static final String SUBNET_A_HOST_2 = "2001:db8:85a3:0:ffff:ffff:ffff:ffff";

	private static final String SUBNET_B_HOST_1 = "2001:db8:85a3:1::1";

	// 앞단 프록시가 없는 환경: 소켓 주소(getRemoteAddress)를 그대로 쓴다.
	private final KeyResolver resolver = new RateLimitConfig().clientIpKeyResolver(false, "X-Real-IP");

	// 앞단에 신뢰 프록시(Caddy)가 있는 환경: 프록시가 넣어준 X-Real-IP 헤더를 클라이언트 IP로 쓴다.
	private final KeyResolver proxyResolver = new RateLimitConfig().clientIpKeyResolver(true, "X-Real-IP");

	@Test
	@DisplayName("IPv4 주소는 \"라우트id:v4:주소\" 형태의 키가 된다")
	void success_ipv4() {
		ServerWebExchange exchange = exchangeOf("203.0.113.5", "user-service-login");

		StepVerifier.create(resolver.resolve(exchange))
				.expectNext("user-service-login:v4:203.0.113.5")
				.verifyComplete();
	}

	@Test
	@DisplayName("IPv6 주소는 앞 64비트(prefix)까지만 키에 사용된다")
	void success_ipv6TruncatesToPrefix() {
		ServerWebExchange exchange = exchangeOf(SUBNET_A_HOST_1, "user-service-login");

		StepVerifier.create(resolver.resolve(exchange))
				.expectNext("user-service-login:v6:20010db885a30000")
				.verifyComplete();
	}

	@Test
	@DisplayName("같은 /64 대역 안의 서로 다른 IPv6 주소는 같은 키를 공유한다 - 대역 안에서 주소를 바꿔 한도를 우회하지 못하게 하는 핵심 동작이다")
	void success_ipv6SamePrefixSharesKey() {
		ServerWebExchange first = exchangeOf(SUBNET_A_HOST_1, "user-service-login");
		ServerWebExchange second = exchangeOf(SUBNET_A_HOST_2, "user-service-login");

		String firstKey = resolver.resolve(first).block();
		String secondKey = resolver.resolve(second).block();

		assertThat(firstKey).isEqualTo(secondKey);
	}

	@Test
	@DisplayName("다른 /64 대역의 IPv6 주소는 다른 키가 된다")
	void success_ipv6DifferentPrefixDiffersKey() {
		ServerWebExchange subnetA = exchangeOf(SUBNET_A_HOST_1, "user-service-login");
		ServerWebExchange subnetB = exchangeOf(SUBNET_B_HOST_1, "user-service-login");

		String keyA = resolver.resolve(subnetA).block();
		String keyB = resolver.resolve(subnetB).block();

		assertThat(keyA).isNotEqualTo(keyB);
	}

	@Test
	@DisplayName("같은 IP 라도 매칭된 라우트가 다르면 다른 키가 된다 - 라우트별로 버킷이 섞이지 않게 하는 핵심 동작이다")
	void success_differentRouteSameIpDiffersKey() {
		ServerWebExchange loginRoute = exchangeOf("203.0.113.5", "user-service-login");
		ServerWebExchange signupRoute = exchangeOf("203.0.113.5", "user-service-signup");

		String loginKey = resolver.resolve(loginRoute).block();
		String signupKey = resolver.resolve(signupRoute).block();

		assertThat(loginKey).isNotEqualTo(signupKey);
	}

	@Test
	@DisplayName("원격 주소를 확인할 수 없으면 unknown 으로 대체된다")
	void success_noRemoteAddressFallsBackToUnknown() {
		ServerWebExchange exchange = exchangeWithAttributesOnly("user-service-login");

		StepVerifier.create(resolver.resolve(exchange))
				.expectNext("user-service-login:unknown")
				.verifyComplete();
	}

	@Test
	@DisplayName("해석되지 않은(호스트명만 있고 주소가 없는) 소켓 주소도 unknown 으로 대체된다")
	void success_unresolvedRemoteAddressFallsBackToUnknown() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
				.remoteAddress(InetSocketAddress.createUnresolved("client.example.com", 12345))
				.build();

		ServerWebExchange exchange = withRoute(MockServerWebExchange.from(request), "user-service-login");

		StepVerifier.create(resolver.resolve(exchange))
				.expectNext("user-service-login:unknown")
				.verifyComplete();
	}

	@Test
	@DisplayName("매칭된 라우트 정보가 없으면 unknown-route 접두사가 붙는다")
	void success_noRouteAttributeUsesUnknownRoutePrefix() {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
				.remoteAddress(new InetSocketAddress("203.0.113.5", 54321))
				.build();

		ServerWebExchange exchange = MockServerWebExchange.from(request);

		StepVerifier.create(resolver.resolve(exchange))
				.expectNext("unknown-route:v4:203.0.113.5")
				.verifyComplete();
	}

	@Test
	@DisplayName("프록시 신뢰 시 소켓 주소 대신 X-Real-IP 헤더의 IP로 키를 만든다 - Caddy 뒤에서 실제 클라이언트별로 세는 핵심 동작이다")
	void success_trustProxyUsesRealIpHeader() {
		// 헤더 = 프록시가 채운 실제 클라이언트 IP, 소켓 = 프록시(Caddy) IP
		ServerWebExchange exchange = exchangeWithRealIp("203.0.113.5", "10.0.0.1", "user-service-login");

		StepVerifier.create(proxyResolver.resolve(exchange))
				.expectNext("user-service-login:v4:203.0.113.5")
				.verifyComplete();
	}

	@Test
	@DisplayName("프록시 신뢰가 꺼져 있으면 X-Real-IP 헤더를 무시하고 소켓 주소를 쓴다 - 헤더 위조로 한도를 우회하지 못하게 하는 핵심 동작이다")
	void success_untrustedIgnoresRealIpHeader() {
		// 헤더에 실제 클라이언트 IP 가 들어와도 신뢰하지 않으므로 소켓(203.0.113.5)으로 센다
		ServerWebExchange exchange = exchangeWithRealIp("10.0.0.1", "203.0.113.5", "user-service-login");

		StepVerifier.create(resolver.resolve(exchange))
				.expectNext("user-service-login:v4:203.0.113.5")
				.verifyComplete();
	}

	@Test
	@DisplayName("프록시 신뢰 상태에서 X-Real-IP 가 없으면 소켓 주소로 폴백한다")
	void success_trustProxyFallsBackToSocketWhenHeaderMissing() {
		ServerWebExchange exchange = exchangeOf("10.0.0.1", "user-service-login");

		StepVerifier.create(proxyResolver.resolve(exchange))
				.expectNext("user-service-login:v4:10.0.0.1")
				.verifyComplete();
	}

	@Test
	@DisplayName("프록시 신뢰 상태에서 X-Real-IP 가 IP 리터럴이 아니면(호스트명 등) DNS 조회 없이 소켓 주소로 폴백한다")
	void success_trustProxyFallsBackWhenHeaderNotIpLiteral() {
		ServerWebExchange exchange = exchangeWithRealIp("evil.example.com", "203.0.113.5", "user-service-login");

		StepVerifier.create(proxyResolver.resolve(exchange))
				.expectNext("user-service-login:v4:203.0.113.5")
				.verifyComplete();
	}

	private static ServerWebExchange exchangeWithRealIp(String realIp, String socketHost, String routeId) {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
				.header("X-Real-IP", realIp)
				.remoteAddress(new InetSocketAddress(socketHost, 54321))
				.build();

		return withRoute(MockServerWebExchange.from(request), routeId);
	}

	private static ServerWebExchange exchangeOf(String host, String routeId) {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
				.remoteAddress(new InetSocketAddress(host, 54321))
				.build();

		return withRoute(MockServerWebExchange.from(request), routeId);
	}

	private static ServerWebExchange exchangeWithAttributesOnly(String routeId) {
		MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login").build();

		return withRoute(MockServerWebExchange.from(request), routeId);
	}

	private static ServerWebExchange withRoute(ServerWebExchange exchange, String routeId) {
		Route route = mock(Route.class);
		given(route.getId()).willReturn(routeId);

		exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR, route);

		return exchange;
	}

}
