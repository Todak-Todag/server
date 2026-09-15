package com.todak_todag.api_gateway.config;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HexFormat;

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
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

	// 주소를 못 구했을 때 쓰는 값. 빈 키를 반환하면 denyEmptyKey 기본 동작으로 요청이 막히는데
	// 정상 요청까지 차단될 수 있어 별도 버킷 하나로 몰아서 제한한다.
	private static final String UNKNOWN_CLIENT_KEY = "unknown";

	// ===== 무엇을 기준으로 셀 것인가 =====

	@Bean
	public KeyResolver clientIpKeyResolver() {
		return exchange -> {
			InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();

			if(remoteAddress == null || remoteAddress.getAddress() == null) {
				return Mono.just(routePrefix(exchange) + UNKNOWN_CLIENT_KEY);
			}

			return Mono.just(routePrefix(exchange) + normalize(remoteAddress.getAddress()));
		};
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