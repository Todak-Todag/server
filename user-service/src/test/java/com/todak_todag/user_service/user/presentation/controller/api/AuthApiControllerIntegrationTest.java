package com.todak_todag.user_service.user.presentation.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.config.MasterAccountInitializer;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;
import com.todak_todag.user_service.user.domain.entity.Region;
import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaAuthRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentVersionRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaRegionRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaUserRepository;
import com.todak_todag.user_service.user.presentation.request.UserLoginRequest;
import com.todak_todag.user_service.user.presentation.request.UserSignupRequest;
import com.todak_todag.user_service.user.presentation.request.UserSignupRequest.AgreementRequest;
import com.todak_todag.user_service.support.PostgresRedisTestSupport;

import jakarta.servlet.http.Cookie;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("Auth/User API 웹 단 통합테스트")
class AuthApiControllerIntegrationTest extends PostgresRedisTestSupport {

	private static final String RAW_PASSWORD = "Test1234!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JpaUserRepository jpaUserRepository;

	@Autowired
	private JpaRegionRepository jpaRegionRepository;

	@Autowired
	private JpaAuthRepository jpaAuthRepository;

	@Autowired
	private JpaConsentDocumentRepository jpaConsentDocumentRepository;

	@Autowired
	private JpaConsentDocumentVersionRepository jpaConsentDocumentVersionRepository;

	@Autowired
	private PasswordEncoderPort passwordEncoder;

	@Autowired
	private TokenPort tokenPort;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Value("${authentication.access-token.redis-key-prefix}")
	private String accessKeyPrefix;

	@Value("${authentication.user.redis-key-prefix}")
	private String userKeyPrefix;

	@Value("${master.username}")
	private String masterUsername;

	@Value("${master.id}")
	private String masterId;

	@Autowired
	private MasterAccountInitializer masterAccountInitializer;

	// Set-Cookie 헤더 원문에서 특정 쿠키의 값만 뽑아낸다
	private String extractCookieValue(MvcResult result, String cookieName) {
		return result.getResponse().getHeaders("Set-Cookie").stream()
				.filter(header -> header.startsWith(cookieName + "="))
				.findFirst()
				.map(header -> header.substring((cookieName + "=").length()).split(";", 2)[0])
				.orElseThrow(() -> new AssertionError(cookieName + " 쿠키가 응답에 없습니다."));
	}

	private String accessKeyOf(String accessToken) {
		return accessKeyPrefix + tokenPort.hashToken(accessToken);
	}

	// 사용자별 AccessToken 역인덱스(Set) 키
	private String userKeyOf(UUID userId) {
		return userKeyPrefix + userId;
	}

	// Set-Cookie 헤더 원문 전체(속성 포함)를 가져온다
	private String extractSetCookieHeader(MvcResult result, String cookieName) {
		return result.getResponse().getHeaders("Set-Cookie").stream()
				.filter(header -> header.startsWith(cookieName + "="))
				.findFirst()
				.orElseThrow(() -> new AssertionError(cookieName + " 쿠키가 응답에 없습니다."));
	}

	// 회원가입 시 지역 존재/활성 검증을 통과시키기 위한 서비스 지원 지역을 만든다
	private Region saveAvailableRegion() {
		Region region = Region.create("전라남도", "고흥군", "4677000000");
		region.updateActive(true);

		return jpaRegionRepository.save(region);
	}

	// 회원가입 시 필수 동의 대상이 되는, 현재 시행 중인 약관(문서+버전)을 만들고 버전 id를 돌려준다
	private UUID saveCurrentRequiredConsentDocumentVersion() {
		ConsentDocument consentDocument = jpaConsentDocumentRepository.save(
				ConsentDocument.create(ConsentDocument.ConsentType.PERSONAL_INFORMATION, "개인정보 수집 이용 동의", true)
		);

		ConsentDocumentVersion version = jpaConsentDocumentVersionRepository.save(
				ConsentDocumentVersion.create(
						consentDocument.getId(), "v1", "약관 내용", LocalDateTime.now().minusDays(1)
				)
		);

		return version.getId();
	}

	private User saveApprovedUser(String username) {
		User user = User.createAdmin(
				UUID.randomUUID(),
				username,
				passwordEncoder.encode(RAW_PASSWORD),
				"테스트유저",
				"01012345670"
		);

		return jpaUserRepository.save(user);
	}

	@Nested
	@DisplayName("마스터 계정 부트스트랩")
	class MasterBootstrap {

		@Test
		@DisplayName("서버 기동 시 설정된 마스터 계정이 설정된 ID로 MASTER/APPROVED 상태로 존재한다")
		void masterAccountTest_exists() {
			Optional<User> master = jpaUserRepository.findByUsernameAndStatusInAndDeletedAtIsNull(
					masterUsername,
					List.of(UserStatus.APPROVED)
			);

			assertThat(master).isPresent();
			assertThat(master.get().getId()).isEqualTo(UUID.fromString(masterId));
			assertThat(master.get().getRole()).isEqualTo(UserRole.MASTER);
			assertThat(master.get().getStatus()).isEqualTo(UserStatus.APPROVED);
		}

		@Test
		@DisplayName("초기화가 다시 실행돼도 예외 없이 마스터 계정이 중복 생성되지 않는다")
		void masterAccountTest_reinitDoesNotDuplicate() {
			long beforeCount = jpaUserRepository.count();

			assertThatCode(() -> masterAccountInitializer.run())
					.doesNotThrowAnyException();

			assertThat(jpaUserRepository.count()).isEqualTo(beforeCount);
		}
	}

	@Nested
	@DisplayName("회원가입")
	class Signup {

		@Test
		@DisplayName("정상 요청이면 201과 함께 PENDING 상태로 저장된다")
		void signupTest_success() throws Exception {
			Region region = saveAvailableRegion();
			UUID consentDocumentVersionId = saveCurrentRequiredConsentDocumentVersion();

			UserSignupRequest request = new UserSignupRequest(
					UserRole.HOSPITAL_STAFF,
					"signuptest1",
					RAW_PASSWORD,
					"홍길동",
					"01011112222",
					region.getId(),
					List.of(new AgreementRequest(consentDocumentVersionId, true))
			);

			mockMvc.perform(post("/api/v1/users/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.userId").exists());

			Optional<User> saved = jpaUserRepository.findByUsernameAndStatusInAndDeletedAtIsNull(
					"signuptest1",
					List.of(UserStatus.PENDING)
			);

			assertThat(saved).isPresent();
			assertThat(saved.get().getStatus()).isEqualTo(UserStatus.PENDING);
		}
	}

	@Nested
	@DisplayName("운영자 등록")
	class AdminCreate {

//		@Test // 지역 단 건 조회 생기면 주석 해제
//		@DisplayName("MASTER 권한으로 요청하면 200과 함께 APPROVED 상태로 저장된다")
//		void adminCreateTest_success() throws Exception {
//			UserAdminCreateRequest request = new UserAdminCreateRequest(
//					"admintest1",
//					RAW_PASSWORD,
//					"관리자",
//					"01022223333",
//					UUID.randomUUID()
//			);
//
//			mockMvc.perform(post("/api/v1/admin/users")
//					.header("X-User-Id", UUID.randomUUID().toString())
//					.header("X-User-Role", UserRole.MASTER.name())
//					.contentType(MediaType.APPLICATION_JSON)
//					.content(objectMapper.writeValueAsString(request)))
//					.andExpect(status().isOk())
//					.andExpect(jsonPath("$.success").value(true))
//					.andExpect(jsonPath("$.data.userId").exists());
//
//			Optional<User> saved = jpaUserRepository.findByUsernameAndStatusIn(
//					"admintest1",
//					List.of(UserStatus.APPROVED)
//			);
//
//			assertThat(saved).isPresent();
//			assertThat(saved.get().getRole()).isEqualTo(UserRole.ADMIN);
//		}
	}

	@Nested
	@DisplayName("로그인")
	class Login {

		@Test
		@DisplayName("성공하면 204와 함께 AccessToken/RefreshToken 쿠키가 올바른 속성으로 내려온다")
		void loginTest_success_setsCookiesWithExpectedAttributes() throws Exception {
			String username = "logintest1";
			saveApprovedUser(username);

			MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UserLoginRequest(username, RAW_PASSWORD))))
					.andExpect(status().isNoContent())
					.andReturn();

			String accessCookie = extractSetCookieHeader(result, "AccessToken");
			String refreshCookie = extractSetCookieHeader(result, "RefreshToken");

			for (String cookie : List.of(accessCookie, refreshCookie)) {
				assertThat(cookie).contains("HttpOnly");
				assertThat(cookie).contains("SameSite=Strict");
				assertThat(cookie).doesNotContain("Secure"); // 로컬/테스트 profile = false
			}

			assertThat(accessCookie).contains("Max-Age=1800"); // jwt.access.max-age = 30m
			assertThat(refreshCookie).contains("Max-Age=604800"); // jwt.refresh.max-age = 7d
		}

		@Test
		@DisplayName("존재하지 않는 아이디면 409 USER_LOGIN_MISMATCHED를 반환한다")
		void loginTest_userNotFound() throws Exception {
			mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UserLoginRequest("no-such-user", RAW_PASSWORD))))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.error.errorCode").value("USER_LOGIN_MISMATCHED"));
		}

		@Test
		@DisplayName("비밀번호가 틀리면 409 USER_LOGIN_MISMATCHED를 반환한다")
		void loginTest_passwordMismatch() throws Exception {
			String username = "logintest2";
			saveApprovedUser(username);

			mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UserLoginRequest(username, "wrong-password!"))))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.error.errorCode").value("USER_LOGIN_MISMATCHED"));
		}

		@Test
		@DisplayName("PENDING 상태 계정이면 403 USER_NOT_APPROVAL을 반환한다")
		void loginTest_pendingUser() throws Exception {
			String username = "logintest3";
			User pendingUser = User.createSignup(
					UUID.randomUUID(),
					username,
					passwordEncoder.encode(RAW_PASSWORD),
					"대기중",
					"01033334444",
					UserRole.HOSPITAL_STAFF
			);
			jpaUserRepository.save(pendingUser);

			mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UserLoginRequest(username, RAW_PASSWORD))))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.error.errorCode").value("USER_NOT_APPROVAL"));
		}

		@Test
		@DisplayName("같은 유저가 두 번 로그인해도 활성 세션은 1개로 유지되고 refreshToken만 갱신된다")
		void loginTest_repeatedLogin_keepsSingleActiveSession() throws Exception {
			String username = "logintest4";
			User user = saveApprovedUser(username);

			mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UserLoginRequest(username, RAW_PASSWORD))))
					.andExpect(status().isNoContent());

			List<Auth> afterFirstLogin = jpaAuthRepository.findAll().stream()
					.filter(auth -> auth.getUserId().equals(user.getId()))
					.toList();

			assertThat(afterFirstLogin).hasSize(1);
			String firstRefreshHash = afterFirstLogin.get(0).getRefreshTokenHash();

			mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UserLoginRequest(username, RAW_PASSWORD))))
					.andExpect(status().isNoContent());

			List<Auth> afterSecondLogin = jpaAuthRepository.findAll().stream()
					.filter(auth -> auth.getUserId().equals(user.getId()))
					.toList();

			assertThat(afterSecondLogin).hasSize(1);
			assertThat(afterSecondLogin.get(0).getRefreshTokenHash()).isNotEqualTo(firstRefreshHash);
		}

		@Test
		@DisplayName("로그인에 성공하면 Redis에 AccessToken 해시 키로 JWT가 저장되고 TTL이 설정된다")
		void loginTest_success_storesAccessTokenInRedis() throws Exception {
			String username = "logintest5";
			saveApprovedUser(username);

			MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UserLoginRequest(username, RAW_PASSWORD))))
					.andExpect(status().isNoContent())
					.andReturn();

			String accessToken = extractCookieValue(result, "AccessToken");
			String redisKey = accessKeyPrefix + tokenPort.hashToken(accessToken);

			String storedJwt = redisTemplate.opsForValue().get(redisKey);

			assertThat(storedJwt).isNotBlank();
			assertThat(storedJwt.split("\\.")).hasSize(3); // JWT header.payload.signature

			Long ttl = redisTemplate.getExpire(redisKey);
			assertThat(ttl).isPositive();
		}

		@Test
		@DisplayName("로그인에 성공하면 사용자별 역인덱스에 AccessToken 해시가 등록되고 TTL이 설정된다")
		void loginTest_success_registersTokenHashInUserIndex() throws Exception {
			String username = "logintest6";
			User user = saveApprovedUser(username);

			MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UserLoginRequest(username, RAW_PASSWORD))))
					.andExpect(status().isNoContent())
					.andReturn();

			String accessToken = extractCookieValue(result, "AccessToken");

			assertThat(redisTemplate.opsForSet().members(userKeyOf(user.getId())))
					.containsExactly(tokenPort.hashToken(accessToken));

			assertThat(redisTemplate.getExpire(userKeyOf(user.getId()))).isPositive();
		}
	}

	@Nested
	@DisplayName("로그아웃")
	class Logout {

		private MvcResult login(String username) throws Exception {
			return mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UserLoginRequest(username, RAW_PASSWORD))))
					.andExpect(status().isNoContent())
					.andReturn();
		}

		@Test
		@DisplayName("정상 로그아웃하면 204와 함께 AccessToken/RefreshToken 쿠키가 즉시 만료된다")
		void logoutTest_success_expiresCookiesImmediately() throws Exception {
			String username = "logouttest1";
			User user = saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String accessToken = extractCookieValue(loginResult, "AccessToken");

			MvcResult logoutResult = mockMvc.perform(post("/api/v1/auth/logout")
					.header("X-User-Id", user.getId().toString())
					.header("X-User-Role", UserRole.ADMIN.name())
					.cookie(new Cookie("AccessToken", accessToken)))
					.andExpect(status().isNoContent())
					.andReturn();

			String accessCookie = extractSetCookieHeader(logoutResult, "AccessToken");
			String refreshCookie = extractSetCookieHeader(logoutResult, "RefreshToken");

			assertThat(accessCookie).contains("Max-Age=0");
			assertThat(refreshCookie).contains("Max-Age=0");
		}

		@Test
		@DisplayName("정상 로그아웃하면 DB의 Auth 세션이 종료 처리된다")
		void logoutTest_success_marksAuthSessionAsLoggedOut() throws Exception {
			String username = "logouttest2";
			User user = saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String accessToken = extractCookieValue(loginResult, "AccessToken");

			mockMvc.perform(post("/api/v1/auth/logout")
					.header("X-User-Id", user.getId().toString())
					.header("X-User-Role", UserRole.ADMIN.name())
					.cookie(new Cookie("AccessToken", accessToken)))
					.andExpect(status().isNoContent());

			Optional<Auth> auth = jpaAuthRepository.findAll().stream()
					.filter(a -> a.getUserId().equals(user.getId()))
					.findFirst();

			assertThat(auth).isPresent();
			assertThat(auth.get().getLogoutAt()).isNotNull();
		}

		@Test
		@DisplayName("정상 로그아웃하면 Redis에서 AccessToken 항목이 삭제된다")
		void logoutTest_success_deletesAccessTokenFromRedis() throws Exception {
			String username = "logouttest3";
			User user = saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String accessToken = extractCookieValue(loginResult, "AccessToken");
			String redisKey = accessKeyPrefix + tokenPort.hashToken(accessToken);

			assertThat(redisTemplate.opsForValue().get(redisKey)).isNotBlank();

			mockMvc.perform(post("/api/v1/auth/logout")
					.header("X-User-Id", user.getId().toString())
					.header("X-User-Role", UserRole.ADMIN.name())
					.cookie(new Cookie("AccessToken", accessToken)))
					.andExpect(status().isNoContent());

			assertThat(redisTemplate.opsForValue().get(redisKey)).isNull();
		}

		@Test
		@DisplayName("인증 정보 없이 요청하면 403을 반환한다")
		void logoutTest_withoutAuthentication_returnsForbidden() throws Exception {
			mockMvc.perform(post("/api/v1/auth/logout"))
					.andExpect(status().isForbidden());
		}

		@Test
		@DisplayName("정상 로그아웃하면 사용자별 역인덱스에서도 AccessToken 해시가 제거된다")
		void logoutTest_success_removesTokenHashFromUserIndex() throws Exception {
			String username = "logouttest4";
			User user = saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String accessToken = extractCookieValue(loginResult, "AccessToken");

			assertThat(redisTemplate.opsForSet().members(userKeyOf(user.getId())))
					.containsExactly(tokenPort.hashToken(accessToken));

			mockMvc.perform(post("/api/v1/auth/logout")
					.header("X-User-Id", user.getId().toString())
					.header("X-User-Role", UserRole.ADMIN.name())
					.cookie(new Cookie("AccessToken", accessToken)))
					.andExpect(status().isNoContent());

			assertThat(redisTemplate.opsForSet().members(userKeyOf(user.getId()))).isEmpty();
		}
	}

	@Nested
	@DisplayName("토큰 재발급")
	class Reissue {

		private MvcResult login(String username) throws Exception {
			return mockMvc.perform(post("/api/v1/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new UserLoginRequest(username, RAW_PASSWORD))))
					.andExpect(status().isNoContent())
					.andReturn();
		}

		private Auth findSessionOf(User user) {
			return jpaAuthRepository.findAll().stream()
					.filter(auth -> auth.getUserId().equals(user.getId()))
					.findFirst()
					.orElseThrow();
		}

		@Test
		@DisplayName("성공하면 204와 함께 새 AccessToken/RefreshToken 쿠키가 올바른 속성으로 내려온다")
		void reissueTest_success_setsCookiesWithExpectedAttributes() throws Exception {
			String username = "reissuetest1";
			saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String refreshToken = extractCookieValue(loginResult, "RefreshToken");

			MvcResult result = mockMvc.perform(post("/api/v1/auth/reissue")
					.cookie(new Cookie("RefreshToken", refreshToken)))
					.andExpect(status().isNoContent())
					.andReturn();

			String accessCookie = extractSetCookieHeader(result, "AccessToken");
			String refreshCookie = extractSetCookieHeader(result, "RefreshToken");

			for (String cookie : List.of(accessCookie, refreshCookie)) {
				assertThat(cookie).contains("HttpOnly");
				assertThat(cookie).contains("SameSite=Strict");
				assertThat(cookie).doesNotContain("Secure"); // 로컬/테스트 profile = false
			}

			assertThat(accessCookie).contains("Max-Age=1800"); // jwt.access.max-age = 30m
			assertThat(refreshCookie).contains("Max-Age=604800"); // jwt.refresh.max-age = 7d
		}

		@Test
		@DisplayName("성공하면 새로 발급된 AccessToken/RefreshToken은 기존 값과 다르다")
		void reissueTest_success_issuesDifferentTokens() throws Exception {
			String username = "reissuetest2";
			saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String oldAccessToken = extractCookieValue(loginResult, "AccessToken");
			String oldRefreshToken = extractCookieValue(loginResult, "RefreshToken");

			MvcResult reissueResult = mockMvc.perform(post("/api/v1/auth/reissue")
					.cookie(new Cookie("RefreshToken", oldRefreshToken)))
					.andExpect(status().isNoContent())
					.andReturn();

			String newAccessToken = extractCookieValue(reissueResult, "AccessToken");
			String newRefreshToken = extractCookieValue(reissueResult, "RefreshToken");

			assertThat(newAccessToken).isNotEqualTo(oldAccessToken);
			assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
		}

		@Test
		@DisplayName("성공하면 DB 세션의 RefreshToken 해시가 새 값으로 회전된다")
		void reissueTest_success_rotatesStoredRefreshTokenHash() throws Exception {
			String username = "reissuetest3";
			User user = saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String oldRefreshToken = extractCookieValue(loginResult, "RefreshToken");
			String hashBefore = findSessionOf(user).getRefreshTokenHash();

			mockMvc.perform(post("/api/v1/auth/reissue")
					.cookie(new Cookie("RefreshToken", oldRefreshToken)))
					.andExpect(status().isNoContent());

			assertThat(findSessionOf(user).getRefreshTokenHash()).isNotEqualTo(hashBefore);
		}

		@Test
		@DisplayName("성공하면 새 AccessToken이 Redis에 저장된다")
		void reissueTest_success_storesNewAccessTokenInRedis() throws Exception {
			String username = "reissuetest4";
			saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String oldRefreshToken = extractCookieValue(loginResult, "RefreshToken");

			MvcResult reissueResult = mockMvc.perform(post("/api/v1/auth/reissue")
					.cookie(new Cookie("RefreshToken", oldRefreshToken)))
					.andExpect(status().isNoContent())
					.andReturn();

			String newAccessToken = extractCookieValue(reissueResult, "AccessToken");
			String redisKey = accessKeyPrefix + tokenPort.hashToken(newAccessToken);

			String storedJwt = redisTemplate.opsForValue().get(redisKey);

			assertThat(storedJwt).isNotBlank();
			assertThat(storedJwt.split("\\.")).hasSize(3); // JWT header.payload.signature
		}

		@Test
		@DisplayName("재발급된 AccessToken도 사용자별 역인덱스에 등록된다 - 누락되면 정지/탈퇴로 무효화할 수 없다")
		void reissueTest_success_registersNewTokenHashInUserIndex() throws Exception {
			String username = "reissuetest7";
			User user = saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String oldRefreshToken = extractCookieValue(loginResult, "RefreshToken");

			MvcResult reissueResult = mockMvc.perform(post("/api/v1/auth/reissue")
					.cookie(new Cookie("RefreshToken", oldRefreshToken)))
					.andExpect(status().isNoContent())
					.andReturn();

			String newAccessToken = extractCookieValue(reissueResult, "AccessToken");

			assertThat(redisTemplate.opsForSet().members(userKeyOf(user.getId())))
					.contains(tokenPort.hashToken(newAccessToken));
		}

		@Test
		@DisplayName("재발급 후 정지되면 재발급된 AccessToken 도 Redis 에서 함께 삭제된다")
		void reissueTest_afterSuspend_newTokenIsAlsoRevoked() throws Exception {
			String username = "reissuetest8";
			User user = saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String oldRefreshToken = extractCookieValue(loginResult, "RefreshToken");

			MvcResult reissueResult = mockMvc.perform(post("/api/v1/auth/reissue")
					.cookie(new Cookie("RefreshToken", oldRefreshToken)))
					.andExpect(status().isNoContent())
					.andReturn();

			String newAccessToken = extractCookieValue(reissueResult, "AccessToken");
			assertThat(redisTemplate.opsForValue().get(accessKeyOf(newAccessToken))).isNotBlank();

			mockMvc.perform(patch("/api/v1/admin/users/" + user.getId() + "/suspend")
					.header("X-User-Id", UUID.fromString(masterId).toString())
					.header("X-User-Role", UserRole.MASTER.name())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"suspendReason\":\"약관 위반\"}"))
					.andExpect(status().isOk());

			assertThat(redisTemplate.opsForValue().get(accessKeyOf(newAccessToken))).isNull();
			assertThat(redisTemplate.hasKey(userKeyOf(user.getId()))).isFalse();
		}

		@Test
		@DisplayName("회전 이후 예전 RefreshToken으로 재시도하면 401 AUTH_REFRESH_TOKEN_INVALID를 반환한다")
		void reissueTest_reusedOldRefreshToken_returnsInvalid() throws Exception {
			String username = "reissuetest5";
			saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String oldRefreshToken = extractCookieValue(loginResult, "RefreshToken");

			mockMvc.perform(post("/api/v1/auth/reissue")
					.cookie(new Cookie("RefreshToken", oldRefreshToken)))
					.andExpect(status().isNoContent());

			mockMvc.perform(post("/api/v1/auth/reissue")
					.cookie(new Cookie("RefreshToken", oldRefreshToken)))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.error.errorCode").value("AUTH_REFRESH_TOKEN_INVALID"));
		}

		@Test
		@DisplayName("RefreshToken 쿠키 없이 요청하면 401 AUTH_REFRESH_TOKEN_INVALID를 반환한다")
		void reissueTest_withoutRefreshTokenCookie_returnsInvalid() throws Exception {
			mockMvc.perform(post("/api/v1/auth/reissue"))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.error.errorCode").value("AUTH_REFRESH_TOKEN_INVALID"));
		}

		@Test
		@DisplayName("RefreshToken 형식이 올바르지 않으면 401 AUTH_REFRESH_TOKEN_INVALID를 반환한다")
		void reissueTest_malformedRefreshToken_returnsInvalid() throws Exception {
			mockMvc.perform(post("/api/v1/auth/reissue")
					.cookie(new Cookie("RefreshToken", "too-short-token")))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.error.errorCode").value("AUTH_REFRESH_TOKEN_INVALID"));
		}

		@Test
		@DisplayName("만료된 세션의 RefreshToken이면 401 AUTH_REFRESH_TOKEN_EXPIRED를 반환한다")
		void reissueTest_expiredSession_returnsExpired() throws Exception {
			String username = "reissuetest6";
			User user = saveApprovedUser(username);

			MvcResult loginResult = login(username);
			String refreshToken = extractCookieValue(loginResult, "RefreshToken");

			Auth session = findSessionOf(user);
			session.renew(session.getRefreshTokenHash(), LocalDateTime.now().minusMinutes(1));
			jpaAuthRepository.save(session);

			mockMvc.perform(post("/api/v1/auth/reissue")
					.cookie(new Cookie("RefreshToken", refreshToken)))
					.andExpect(status().isUnauthorized())
					.andExpect(jsonPath("$.error.errorCode").value("AUTH_REFRESH_TOKEN_EXPIRED"));
		}
	}
}
