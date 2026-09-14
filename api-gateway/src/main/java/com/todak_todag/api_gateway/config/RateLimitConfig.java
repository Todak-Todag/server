package com.todak_todag.api_gateway.config;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HexFormat;

import org.bouncycastle.util.Arrays;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
	
	private String normalize(InetAddress address) {
		if(address instanceof Inet6Address) {
			byte[] prefix = Arrays.copyOf(address.getAddress(), 8);
			return "v6:" + HexFormat.of().formatHex(prefix);
		}
		
		return "v4:" + address.getHostAddress();
	}
}
