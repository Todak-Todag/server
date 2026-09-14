package com.todak_todag.api_gateway.config;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HexFormat;

import org.bouncycastle.util.Arrays;
import org.springframework.cloud.gateway.filter.ratelimit.Bucket4jRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce;
import io.lettuce.core.api.StatefulRedisConnection;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {
	
	// 주소를 못 구했을 때 쓰는 값. 빈 키를 반환하면 denyEmptyKey 기본 동작으로 요청이 막히는데
	// 정상 요청까지 차단될 수 있어 별도 버킷 하나로 몰아서 제한한다.
	private static final String UNKNOWN_CLIENT_KEY = "unknown";
	
	@Bean
	public KeyResolver clientIpKeyResolver() {
		return exchange -> {
			InetSocketAddress remoteAddress = exchange.getRequest().getRemoteAddress();
			
			if(remoteAddress == null || remoteAddress.getAddress() == null) {
				return Mono.just(UNKNOWN_CLIENT_KEY);
			}
			
			return Mono.just(normalize(remoteAddress.getAddress()));
		};
	}
	
	public AsyncProxyManager<String> rateLimitProxyManager(
			StatefulRedisConnection<String, byte[]> rateLimitRedisConnection,
			RateLimitProperties properties
	) {
		return Bucket4jLettuce.casBasedBuilder(rateLimitRedisConnection)
				.expirationAfterWrite(
						ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(properties.maxIdle())
				)
				.build()
				.asAsync();
	}
	
	@Bean
	public Bucket4jRateLimiter bucket4jRateLimiter(
			AsyncProxyManager<String> rateLimitProxyManager,
			ConfigurationService configurationService,
			RateLimitProperties properties
	) {
		Bucket4jRateLimiter rateLimiter = new Bucket4jRateLimiter(rateLimitProxyManager, configurationService);
		
		properties.buckets().forEach((routeId, limit) -> rateLimiter.getConfig().put(routeId, toConfig(limit)));
		
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
	
	private String normalize(InetAddress address) {
		if(address instanceof Inet6Address) {
			byte[] prefix = Arrays.copyOf(address.getAddress(), 8);
			return "v6:" + HexFormat.of().formatHex(prefix);
		}
		
		return "v4:" + address.getHostAddress();
	}
}
