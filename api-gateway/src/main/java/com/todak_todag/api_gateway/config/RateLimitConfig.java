package com.todak_todag.api_gateway.config;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.ratelimit.Bucket4jRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.ServerWebExchange;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class RateLimitConfig {

	// 주소를 못 구했을 때 쓰는 값. 빈 키를 반환하면 denyEmptyKey 기본 동작으로 요청이 막히는데
	// 정상 요청까지 차단될 수 있어 별도 버킷 하나로 몰아서 제한한다.
	private static final String UNKNOWN_CLIENT_KEY = "unknown";

	// IP 리터럴(IPv4/IPv6)에 쓰이는 문자만 허용해 호스트명이 들어와 DNS 조회가 일어나는 것을 막는다.
	private static final Pattern IP_LITERAL_PATTERN = Pattern.compile("^[0-9A-Fa-f.:%]+$");

	// ===== 무엇을 기준으로 셀 것인가 =====

	@Bean
	public KeyResolver clientIpKeyResolver(
			// 앞단에 신뢰할 수 있는 프록시(Caddy)가 있어 프록시가 넣어준 실제 클라이언트 IP 헤더를
			// 읽어야 하는 환경에서만 true 로 켠다. 게이트웨이가 직접 노출되는 환경에서 true 로 켜면
			// 클라이언트가 헤더를 위조해 한도를 우회할 수 있으므로 기본값은 false 다.
			@Value("${rate-limit.trust-proxy-header:false}") boolean trustProxyHeader,
			@Value("${rate-limit.client-ip-header:X-Real-IP}") String clientIpHeader
	) {
		return exchange -> {
			InetAddress clientAddress = resolveClientAddress(exchange, trustProxyHeader, clientIpHeader);

			if(clientAddress == null) {
				return Mono.just(routePrefix(exchange) + UNKNOWN_CLIENT_KEY);
			}

			return Mono.just(routePrefix(exchange) + normalize(clientAddress));
		};
	}

	private InetAddress resolveClientAddress(
			ServerWebExchange exchange, boolean trustProxyHeader, String clientIpHeader
	) {
		if(trustProxyHeader) {
			String headerValue = exchange.getRequest().getHeaders().getFirst(clientIpHeader);

			if(headerValue != null && !headerValue.isBlank()) {
				InetAddress fromHeader = parseIpLiteral(headerValue.trim());

				if(fromHeader != null) {
					return fromHeader;
				}

				// 프록시가 채워주기로 한 헤더가 비어있거나 형식이 이상하다. 프록시 설정이 어긋났다는 신호라
				// 로그만 남기고 소켓 주소(대개 프록시 IP)로 폴백한다.
				log.warn(
						"[Gateway] 신뢰 프록시 헤더 {} 값을 IP로 해석하지 못했습니다. socket 주소로 폴백합니다.",
						clientIpHeader
				);
			}
		}

		InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();

		return remoteAddress != null ? remoteAddress.getAddress() : null;
	}

	// 값은 신뢰 프록시가 넣어준 IP 리터럴이라는 전제다. IP 리터럴에 쓰이는 문자만 허용해
	// 호스트명이 들어와 DNS 조회가 발생하는 것을 원천 차단하고, 그 뒤 InetAddress 로 해석한다.
	// 리터럴이 아니거나 해석에 실패하면 null 을 돌려 unknown 버킷으로 보낸다.
	private InetAddress parseIpLiteral(String value) {
		if(!IP_LITERAL_PATTERN.matcher(value).matches()) {
			return null;
		}

		try {
			return InetAddress.getByName(value);
		} catch (UnknownHostException e) {
			return null;
		}
	}

	private String routePrefix(ServerWebExchange exchange) {
		Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
		
		return (route != null ? route.getId() : "unknown-route") + ":";
	}

	private String normalize(InetAddress address) {
		if(address instanceof Inet6Address) {
			byte[] prefix = Arrays.copyOf(address.getAddress(), 8);

			return "v6:" + HexFormat.of().formatHex(prefix);
		}

		return "v4:" + address.getHostAddress();
	}

	// ===== 버킷 상태를 어디에 둘 것인가 =====

	@Bean(destroyMethod = "shutdown")
	public RedisClient rateLimitRedisClient(
			@Value("${spring.data.redis.host}") String host,
			@Value("${spring.data.redis.port}") int port,
			@Value("${spring.data.redis.password:}") String password
	) {
		RedisURI.Builder uriBuilder = RedisURI.builder()
				.withHost(host)
				.withPort(port);

		if(!password.isBlank()) {
			uriBuilder.withPassword(password.toCharArray());
		}

		return RedisClient.create(uriBuilder.build());
	}

	@Bean(destroyMethod = "close")
	public StatefulRedisConnection<String, byte[]> rateLimitRedisConnection(RedisClient rateLimitRedisClient) {
		return rateLimitRedisClient.connect(
				RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
		);
	}

	// ===== 얼마나 허용할 것인가 =====

	@Bean
	public Bucket4jRateLimiter bucket4jRateLimiter(
			StatefulRedisConnection<String, byte[]> rateLimitRedisConnection,
			ConfigurationService configurationService,
			RateLimitProperties properties
	) {
		AsyncProxyManager<String> proxyManager = Bucket4jLettuce.casBasedBuilder(rateLimitRedisConnection)
				.expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(properties.maxIdle()))
				.build()
				.asAsync();
		
		Bucket4jRateLimiter rateLimiter = new Bucket4jRateLimiter(proxyManager, configurationService);

		properties.buckets().forEach(
				(routeId, limit) -> rateLimiter.getConfig().put(routeId, toConfig(limit))
		);

		return rateLimiter;
	}

	private Bucket4jRateLimiter.Config toConfig(RateLimitProperties.Limit limit) {
		return new Bucket4jRateLimiter.Config()
				.setCapacity(limit.capacity())
				.setRefillTokens(limit.refillTokens())
				.setRefillPeriod(limit.refillPeriod())
				.setRefillStyle(Bucket4jRateLimiter.RefillStyle.GREEDY)
				.setRequestedTokens(1);
	}
}