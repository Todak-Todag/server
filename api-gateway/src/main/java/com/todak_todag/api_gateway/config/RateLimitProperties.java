package com.todak_todag.api_gateway.config;

import java.time.Duration;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rate-limit")
public record RateLimitProperties(Duration maxIdle, Map<String, Limit> buckets) {

	public RateLimitProperties {
		requireMaxIdle(maxIdle);
		requireBuckets(buckets);
	}
	
	public record Limit(long capacity, long refillTokens, Duration refillPeriod) {}

	private static void requireMaxIdle(Duration maxIdle) {
		if(maxIdle == null || maxIdle.isNegative() || maxIdle.isZero()) {
			throw new IllegalArgumentException(
					"[Gateway] 서버 구동 실패. rate-limit.max-idle 은 0보다 커야 합니다."
			);
		}
	}

	private static void requireBuckets(Map<String, Limit> buckets) {
		if(buckets == null || buckets.isEmpty()) {
			throw new IllegalArgumentException(
					"[Gateway] 서버 구동 실패. rate-limit.buckets 에 최소 하나의 설정이 필요합니다."
			);
		}

		buckets.forEach(RateLimitProperties::requireLimit);
	}

	private static void requireLimit(String name, Limit limit) {
		if(limit.capacity() <= 0) {
			throw new IllegalArgumentException(
					"[Gateway] 서버 구동 실패. rate-limit.buckets." + name + ".capacity 는 0보다 커야 합니다."
			);
		}

		if(limit.refillTokens() <= 0) {
			throw new IllegalArgumentException(
					"[Gateway] 서버 구동 실패. rate-limit.buckets." + name + ".refill-tokens 는 0보다 커야 합니다."
			);
		}

		if(limit.refillPeriod() == null || limit.refillPeriod().isNegative() || limit.refillPeriod().isZero()) {
			throw new IllegalArgumentException(
					"[Gateway] 서버 구동 실패. rate-limit.buckets." + name + ".refill-period 는 0보다 커야 합니다."
			);
		}
	}
}