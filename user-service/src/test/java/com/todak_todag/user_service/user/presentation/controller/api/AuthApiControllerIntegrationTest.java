package com.todak_todag.user_service.user.presentation.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.user.application.port.PasswordEncoderPort;
import com.todak_todag.user_service.user.application.port.TokenPort;
import com.todak_todag.user_service.user.domain.entity.auth.Auth;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaAuthRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaUserRepository;
import com.todak_todag.user_service.user.presentation.request.UserRequest.UserAdminCreateRequest;
import com.todak_todag.user_service.user.presentation.request.UserRequest.UserLoginRequest;
import com.todak_todag.user_service.user.presentation.request.UserRequest.UserSignupRequest;
import com.todak_todag.user_service.user.presentation.request.UserRequest.UserSignupRequest.AgreementRequest;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("Auth/User API 웹 단 통합테스트")
class AuthApiControllerIntegrationTest {

	private static final String RAW_PASSWORD = "Test1234!";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JpaUserRepository jpaUserRepository;

	@Autowired
	private JpaAuthRepository jpaAuthRepository;

	@Autowired
	private PasswordEncoderPort passwordEncoder;

	@Autowired
	private TokenPort tokenPort;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	@Value("${authentication.access-token.redis-key-prefix}")
	private String accessKeyPrefix;

	@Value("${master.username}")
	private String masterUsername;

	// Set-Cookie 헤더 원문에서 특정 쿠키의 값만 뽑아낸다
	private String extractCookieValue(MvcResult result, String cookieName) {
		return result.getResponse().getHeaders("Set-Cookie").stream()
				.filter(header -> header.startsWith(cookieName + "="))
				.findFirst()
				.map(header -> header.substring((cookieName + "=").length()).split(";", 2)[0])
				.orElseThrow(() -> new AssertionError(cookieName + " 쿠키가 응답에 없습니다."));
	}

	// Set-Cookie 헤더 원문 전체(속성 포함)를 가져온다
	private String extractSetCookieHeader(MvcResult result, String cookieName) {
		return result.getResponse().getHeaders("Set-Cookie").stream()
				.filter(header -> header.startsWith(cookieName + "="))
				.findFirst()
				.orElseThrow(() -> new AssertionError(cookieName + " 쿠키가 응답에 없습니다."));
	}

	private User saveApprovedUser(String username) {
		User user = User.createAdmin(
				UUID.randomUUID(),
				username,
				passwordEncoder.encode(RAW_PASSWORD),
				"테스트유저",
				"01012345670",
				UserRole.ADMIN
		);

		return jpaUserRepository.save(user);
	}

	@Nested
	@DisplayName("마스터 계정 부트스트랩")
	class MasterBootstrap {

		@Test
		@DisplayName("서버 기동 시 설정된 마스터 계정이 MASTER/APPROVED 상태로 존재한다")
		void masterAccountTest_exists() {
			Optional<User> master = jpaUserRepository.findByUsernameAndStatusIn(
					masterUsername,
					List.of(UserStatus.APPROVED)
			);

			assertThat(master).isPresent();
			assertThat(master.get().getRole()).isEqualTo(UserRole.MASTER);
			assertThat(master.get().getStatus()).isEqualTo(UserStatus.APPROVED);
		}
	}

	@Nested
	@DisplayName("회원가입")
	class Signup {

		@Test
		@DisplayName("정상 요청이면 201과 함께 PENDING 상태로 저장된다")
		void signupTest_success() throws Exception {
			UserSignupRequest request = new UserSignupRequest(
					UserRole.HOSPITAL_STAFF,
					"signuptest1",
					RAW_PASSWORD,
					"홍길동",
					"01011112222",
					UUID.randomUUID(),
					List.of(new AgreementRequest(UUID.randomUUID(), true))
			);

			mockMvc.perform(post("/api/v1/users/signup")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(request)))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.data.userId").exists());

			Optional<User> saved = jpaUserRepository.findByUsernameAndStatusIn(
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
				assertThat(cookie).contains("Max-Age=604800"); // 7일
				assertThat(cookie).doesNotContain("Secure"); // 로컬/테스트 profile = false
			}
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
	}
}
