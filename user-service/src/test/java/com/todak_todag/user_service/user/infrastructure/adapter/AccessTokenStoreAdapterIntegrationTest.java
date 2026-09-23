package com.todak_todag.user_service.user.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.todak_todag.user_service.support.PostgresRedisTestSupport;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.application.port.TokenStorePort;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("AccessTokenStoreAdapter Redis 통합테스트")
class AccessTokenStoreAdapterIntegrationTest extends PostgresRedisTestSupport {

	private static final String JWT = "header.payload.signature";

	@Autowired
	private TokenStorePort tokenStorePort;

	@Autowired
	private TokenPort tokenPort;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Value("${authentication.access-token.redis-key-prefix}")
	private String accessKeyPrefix;

	@Value("${authentication.user.redis-key-prefix}")
	private String userKeyPrefix;

	@Value("${jwt.access.max-age}")
	private Duration accessTokenTtl;

	private String accessKey(String accessToken) {
		return accessKeyPrefix + tokenPort.hashToken(accessToken);
	}

	private String userKey(UUID userId) {
		return userKeyPrefix + userId;
	}

	// 테스트마다 고유한 사용자/토큰을 쓰므로 키가 서로 간섭하지 않는다
	private static String uniqueToken() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	@Nested
	@DisplayName("AccessToken 저장")
	class StoreAccessToken {

		@Test
		@DisplayName("AccessToken 키와 사용자 역인덱스가 함께 생성된다")
		void store_createsBothKeys() {
			// given
			UUID userId = UUID.randomUUID();
			String accessToken = uniqueToken();

			// when
			tokenStorePort.storeAccessToken(userId, accessToken, JWT);

			// then
			assertThat(redisTemplate.opsForValue().get(accessKey(accessToken))).isEqualTo(JWT);
			assertThat(redisTemplate.opsForSet().members(userKey(userId)))
					.containsExactly(tokenPort.hashToken(accessToken));
		}

		@Test
		@DisplayName("AccessToken 키와 역인덱스 모두 TTL 이 설정된다")
		void store_setsTtlOnBothKeys() {
			// given
			UUID userId = UUID.randomUUID();
			String accessToken = uniqueToken();

			// when
			tokenStorePort.storeAccessToken(userId, accessToken, JWT);

			// then
			assertThat(redisTemplate.getExpire(accessKey(accessToken))).isPositive();
			assertThat(redisTemplate.getExpire(userKey(userId))).isPositive();
		}

		@Test
		@DisplayName("같은 사용자가 여러 번 로그인하면 역인덱스에 해시가 누적된다")
		void store_accumulatesHashesForSameUser() {
			// given
			UUID userId = UUID.randomUUID();
			String firstToken = uniqueToken();
			String secondToken = uniqueToken();

			// when
			tokenStorePort.storeAccessToken(userId, firstToken, JWT);
			tokenStorePort.storeAccessToken(userId, secondToken, JWT);

			// then
			assertThat(redisTemplate.opsForSet().members(userKey(userId)))
					.containsExactlyInAnyOrder(
							tokenPort.hashToken(firstToken),
							tokenPort.hashToken(secondToken)
					);
		}
	}

	@Nested
	@DisplayName("임시 AccessToken 저장")
	class StoreAccessTokenTemp {

		@Test
		@DisplayName("AccessToken 키에는 짧은 TTL 이, 역인덱스에는 AccessToken 최대 수명이 적용된다")
		void storeTemp_doesNotShortenUserIndexTtl() {
			// given
			UUID userId = UUID.randomUUID();
			String accessToken = uniqueToken();
			Duration tempTtl = Duration.ofMinutes(3);

			// when
			tokenStorePort.storeAccessTokenTemp(userId, accessToken, JWT, tempTtl);

			// then
			Long accessTtl = redisTemplate.getExpire(accessKey(accessToken));
			Long indexTtl = redisTemplate.getExpire(userKey(userId));

			assertThat(accessTtl).isLessThanOrEqualTo(tempTtl.toSeconds());
			// 인덱스 TTL 은 임시 TTL 에 깎이지 않고 AccessToken 최대 수명(기본 30분)을 유지한다
			assertThat(indexTtl).isGreaterThan(tempTtl.toSeconds());
			assertThat(indexTtl).isLessThanOrEqualTo(accessTokenTtl.toSeconds());
		}

		@Test
		@DisplayName("임시 토큰도 역인덱스에 등록되어 무효화 대상이 된다")
		void storeTemp_isRegisteredInUserIndex() {
			// given
			UUID userId = UUID.randomUUID();
			String accessToken = uniqueToken();

			// when
			tokenStorePort.storeAccessTokenTemp(userId, accessToken, JWT, Duration.ofMinutes(3));

			// then
			assertThat(redisTemplate.opsForSet().members(userKey(userId)))
					.containsExactly(tokenPort.hashToken(accessToken));
		}
	}

	@Nested
	@DisplayName("AccessToken 단건 삭제")
	class DeleteAccessToken {

		@Test
		@DisplayName("AccessToken 키를 지우고 역인덱스에서도 해시를 제거한다")
		void delete_removesKeyAndIndexEntry() {
			// given
			UUID userId = UUID.randomUUID();
			String accessToken = uniqueToken();
			tokenStorePort.storeAccessToken(userId, accessToken, JWT);

			// when
			tokenStorePort.deleteAccessToken(userId, accessToken);

			// then
			assertThat(redisTemplate.opsForValue().get(accessKey(accessToken))).isNull();
			assertThat(redisTemplate.opsForSet().members(userKey(userId))).isEmpty();
		}

		@Test
		@DisplayName("여러 세션 중 하나만 지우면 나머지 세션은 유지된다")
		void delete_keepsOtherSessions() {
			// given
			UUID userId = UUID.randomUUID();
			String firstToken = uniqueToken();
			String secondToken = uniqueToken();
			tokenStorePort.storeAccessToken(userId, firstToken, JWT);
			tokenStorePort.storeAccessToken(userId, secondToken, JWT);

			// when
			tokenStorePort.deleteAccessToken(userId, firstToken);

			// then
			assertThat(redisTemplate.opsForValue().get(accessKey(firstToken))).isNull();
			assertThat(redisTemplate.opsForValue().get(accessKey(secondToken))).isEqualTo(JWT);
			assertThat(redisTemplate.opsForSet().members(userKey(userId)))
					.containsExactly(tokenPort.hashToken(secondToken));
		}

		@Test
		@DisplayName("accessToken 이 null 이거나 공백이면 아무 것도 하지 않는다")
		void delete_nullOrBlankIsNoOp() {
			// given
			UUID userId = UUID.randomUUID();
			String accessToken = uniqueToken();
			tokenStorePort.storeAccessToken(userId, accessToken, JWT);

			// when & then
			assertThatCode(() -> tokenStorePort.deleteAccessToken(userId, null)).doesNotThrowAnyException();
			assertThatCode(() -> tokenStorePort.deleteAccessToken(userId, "  ")).doesNotThrowAnyException();

			assertThat(redisTemplate.opsForValue().get(accessKey(accessToken))).isEqualTo(JWT);
			assertThat(redisTemplate.opsForSet().members(userKey(userId))).hasSize(1);
		}
	}

	@Nested
	@DisplayName("사용자 세션 전체 무효화")
	class RevokeAllSessions {

		@Test
		@DisplayName("해당 사용자의 모든 AccessToken 키와 역인덱스가 삭제된다")
		void revoke_deletesAllTokensAndIndex() {
			// given
			UUID userId = UUID.randomUUID();
			String firstToken = uniqueToken();
			String secondToken = uniqueToken();
			String thirdToken = uniqueToken();
			tokenStorePort.storeAccessToken(userId, firstToken, JWT);
			tokenStorePort.storeAccessToken(userId, secondToken, JWT);
			tokenStorePort.storeAccessToken(userId, thirdToken, JWT);

			// when
			tokenStorePort.revokeAllSessions(userId);

			// then
			assertThat(redisTemplate.opsForValue().get(accessKey(firstToken))).isNull();
			assertThat(redisTemplate.opsForValue().get(accessKey(secondToken))).isNull();
			assertThat(redisTemplate.opsForValue().get(accessKey(thirdToken))).isNull();
			assertThat(redisTemplate.hasKey(userKey(userId))).isFalse();
		}

		@Test
		@DisplayName("다른 사용자의 세션은 영향을 받지 않는다")
		void revoke_doesNotAffectOtherUsers() {
			// given
			UUID userId = UUID.randomUUID();
			UUID otherUserId = UUID.randomUUID();
			String token = uniqueToken();
			String otherToken = uniqueToken();
			tokenStorePort.storeAccessToken(userId, token, JWT);
			tokenStorePort.storeAccessToken(otherUserId, otherToken, JWT);

			// when
			tokenStorePort.revokeAllSessions(userId);

			// then
			assertThat(redisTemplate.opsForValue().get(accessKey(token))).isNull();
			assertThat(redisTemplate.opsForValue().get(accessKey(otherToken))).isEqualTo(JWT);
			assertThat(redisTemplate.opsForSet().members(userKey(otherUserId)))
					.containsExactly(tokenPort.hashToken(otherToken));
		}

		@Test
		@DisplayName("활성 세션이 없는 사용자에 대해서도 예외 없이 동작한다")
		void revoke_noSessionIsNoOp() {
			// given
			UUID userId = UUID.randomUUID();

			// when & then
			assertThatCode(() -> tokenStorePort.revokeAllSessions(userId)).doesNotThrowAnyException();
			assertThat(redisTemplate.hasKey(userKey(userId))).isFalse();
		}

		@Test
		@DisplayName("역인덱스에 이미 만료된(죽은) 해시가 남아 있어도 나머지를 정상 삭제한다")
		void revoke_toleratesDeadHashesInIndex() {
			// given
			UUID userId = UUID.randomUUID();
			String liveToken = uniqueToken();
			String deadToken = uniqueToken();

			tokenStorePort.storeAccessToken(userId, liveToken, JWT);
			tokenStorePort.storeAccessToken(userId, deadToken, JWT);

			// AccessToken 키만 먼저 사라진 상황(TTL 만료)을 재현한다 - 역인덱스에는 해시가 남아 있다
			redisTemplate.delete(accessKey(deadToken));
			assertThat(redisTemplate.opsForSet().members(userKey(userId))).hasSize(2);

			// when & then
			assertThatCode(() -> tokenStorePort.revokeAllSessions(userId)).doesNotThrowAnyException();

			assertThat(redisTemplate.opsForValue().get(accessKey(liveToken))).isNull();
			assertThat(redisTemplate.hasKey(userKey(userId))).isFalse();
		}
	}
}
