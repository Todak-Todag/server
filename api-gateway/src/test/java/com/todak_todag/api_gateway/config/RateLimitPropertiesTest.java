package com.todak_todag.api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.todak_todag.api_gateway.config.RateLimitProperties.Limit;

@DisplayName("RateLimitProperties")
class RateLimitPropertiesTest {

	private static final Limit VALID_LIMIT = new Limit(20, 10, Duration.ofMinutes(1));

	@Test
	@DisplayName("유효한 값이면 정상적으로 생성된다")
	void success_1() {
		RateLimitProperties properties = new RateLimitProperties(
				Duration.ofHours(1),
				Map.of("user-service-login", VALID_LIMIT)
		);

		assertThat(properties.maxIdle()).isEqualTo(Duration.ofHours(1));
		assertThat(properties.buckets()).containsKey("user-service-login");
	}

	@Test
	@DisplayName("maxIdle 이 null 이면 생성 시점에 실패한다")
	void fail_maxIdleNull() {
		assertThatThrownBy(() -> new RateLimitProperties(null, Map.of("user-service-login", VALID_LIMIT)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("max-idle");
	}

	@Test
	@DisplayName("maxIdle 이 0이면 생성 시점에 실패한다")
	void fail_maxIdleZero() {
		assertThatThrownBy(() -> new RateLimitProperties(Duration.ZERO, Map.of("user-service-login", VALID_LIMIT)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("max-idle");
	}

	@Test
	@DisplayName("maxIdle 이 음수면 생성 시점에 실패한다")
	void fail_maxIdleNegative() {
		assertThatThrownBy(() -> new RateLimitProperties(Duration.ofMinutes(-1), Map.of("user-service-login", VALID_LIMIT)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("max-idle");
	}

	@Test
	@DisplayName("buckets 가 null 이면 생성 시점에 실패한다")
	void fail_bucketsNull() {
		assertThatThrownBy(() -> new RateLimitProperties(Duration.ofHours(1), null))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("buckets");
	}

	@Test
	@DisplayName("buckets 가 빈 맵이면 생성 시점에 실패한다")
	void fail_bucketsEmpty() {
		assertThatThrownBy(() -> new RateLimitProperties(Duration.ofHours(1), Map.of()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("buckets");
	}

	@Test
	@DisplayName("버킷의 capacity 가 0 이하면 생성 시점에 실패하고, 어떤 버킷인지 메시지에 남는다")
	void fail_bucketCapacityNotPositive() {
		Limit invalid = new Limit(0, 10, Duration.ofMinutes(1));

		assertThatThrownBy(() -> new RateLimitProperties(Duration.ofHours(1), Map.of("user-service-login", invalid)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("user-service-login")
				.hasMessageContaining("capacity");
	}

	@Test
	@DisplayName("버킷의 refillTokens 가 0 이하면 생성 시점에 실패하고, 어떤 버킷인지 메시지에 남는다")
	void fail_bucketRefillTokensNotPositive() {
		Limit invalid = new Limit(20, 0, Duration.ofMinutes(1));

		assertThatThrownBy(() -> new RateLimitProperties(Duration.ofHours(1), Map.of("user-service-login", invalid)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("user-service-login")
				.hasMessageContaining("refill-tokens");
	}

	@Test
	@DisplayName("버킷의 refillPeriod 가 null 이면 생성 시점에 실패한다")
	void fail_bucketRefillPeriodNull() {
		Limit invalid = new Limit(20, 10, null);

		assertThatThrownBy(() -> new RateLimitProperties(Duration.ofHours(1), Map.of("user-service-login", invalid)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("user-service-login")
				.hasMessageContaining("refill-period");
	}

	@Test
	@DisplayName("버킷의 refillPeriod 가 0 이하면 생성 시점에 실패한다")
	void fail_bucketRefillPeriodNotPositive() {
		Limit invalid = new Limit(20, 10, Duration.ZERO);

		assertThatThrownBy(() -> new RateLimitProperties(Duration.ofHours(1), Map.of("user-service-login", invalid)))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("user-service-login")
				.hasMessageContaining("refill-period");
	}

	@Test
	@DisplayName("여러 버킷 중 하나만 잘못돼도, 그 버킷 이름이 메시지에 남는다")
	void fail_onlyOneOfMultipleBucketsInvalid() {
		Limit invalid = new Limit(-1, 10, Duration.ofMinutes(1));

		Map<String, Limit> buckets = Map.of(
				"user-service-login", VALID_LIMIT,
				"user-service-signup", invalid
		);

		assertThatThrownBy(() -> new RateLimitProperties(Duration.ofHours(1), buckets))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("user-service-signup");
	}
}
