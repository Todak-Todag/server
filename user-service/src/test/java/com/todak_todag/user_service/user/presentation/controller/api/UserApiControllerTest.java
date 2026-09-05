package com.todak_todag.user_service.user.presentation.controller.api;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.config.SecurityConfig;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;
import com.todak_todag.user_service.user.application.service.command.UserUpdateService;
import com.todak_todag.user_service.user.application.service.query.UserQueryService;
import com.todak_todag.user_service.user.application.service.result.UserInfoResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserApiController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import(SecurityConfig.class)
class UserApiControllerTest {

	private static final String URI = "/api/v1/users/me";

	private static final UUID USER_ID = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserQueryService userQueryService;

	@MockitoBean
	private UserCreateService userCreateService;

	@MockitoBean
	private UserUpdateService userUpdateService;

	@Nested
	@DisplayName("내 정보 조회")
	class Me {

		@Test
		@DisplayName("정상 헤더로 요청하면 내 정보를 조회한다")
		void meTest_success() throws Exception {
			// given
			UUID regionId = UUID.randomUUID();
			UserInfoResult result = new UserInfoResult(
					"김영수",
					"전라남도",
					"고흥군",
					"01012345678",
					regionId,
					UserRole.PATIENT
			);

			given(userQueryService.getMe(USER_ID)).willReturn(result);

			// when & then
			mockMvc.perform(get(URI)
							.header("X-User-Id", USER_ID.toString())
							.header("X-User-Role", "PATIENT"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.code").value(200))
					.andExpect(jsonPath("$.message").value("내 정보 조회 완료"))
					.andExpect(jsonPath("$.data.name").value("김영수"))
					.andExpect(jsonPath("$.data.province").value("전라남도"))
					.andExpect(jsonPath("$.data.district").value("고흥군"))
					.andExpect(jsonPath("$.data.phone").value("01012345678"))
					.andExpect(jsonPath("$.data.regionId").value(regionId.toString()))
					.andExpect(jsonPath("$.data.role").value("퇴원 예정자"));
		}

		@Test
		@DisplayName("지역이 서비스 지원 지역이 아니면 서비스가 내려준 안내 문구를 그대로 응답한다")
		void meTest_regionNotSupported() throws Exception {
			// given
			UUID regionId = UUID.randomUUID();
			UserInfoResult result = new UserInfoResult(
					"김영수",
					"서비스 이용 지역이 아닙니다.",
					"서비스 이용 지역이 아닙니다.",
					"01012345678",
					regionId,
					UserRole.PATIENT
			);

			given(userQueryService.getMe(USER_ID)).willReturn(result);

			// when & then
			mockMvc.perform(get(URI)
							.header("X-User-Id", USER_ID.toString())
							.header("X-User-Role", "PATIENT"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.province").value("서비스 이용 지역이 아닙니다."))
					.andExpect(jsonPath("$.data.district").value("서비스 이용 지역이 아닙니다."));
		}

		@Test
		@DisplayName("지역 ID가 없는 사용자는 province/district/regionId가 없는 정보를 응답한다")
		void meTest_noRegion() throws Exception {
			// given
			UserInfoResult result = new UserInfoResult(
					"관리자",
					null,
					null,
					"01099998888",
					null,
					UserRole.MASTER
			);

			given(userQueryService.getMe(USER_ID)).willReturn(result);

			// when & then
			mockMvc.perform(get(URI)
							.header("X-User-Id", USER_ID.toString())
							.header("X-User-Role", "MASTER"))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.province").doesNotExist())
					.andExpect(jsonPath("$.data.district").doesNotExist())
					.andExpect(jsonPath("$.data.regionId").doesNotExist());
		}

		@Test
		@DisplayName("인증 헤더 없이 요청하면 인증에 실패하고 서비스를 호출하지 않는다")
		void meTest_fail_unauthenticated() throws Exception {
			// when & then
			mockMvc.perform(get(URI))
					.andExpect(status().is4xxClientError());

			then(userQueryService).should(never()).getMe(any(UUID.class));
		}

		@Test
		@DisplayName("존재하지 않는 사용자면 404 에러 응답을 반환한다")
		void meTest_fail_userNotFound() throws Exception {
			// given
			given(userQueryService.getMe(USER_ID))
					.willThrow(new BusinessException(UserErrorCode.USER_NOT_FOUND));

			// when & then
			mockMvc.perform(get(URI)
							.header("X-User-Id", USER_ID.toString())
							.header("X-User-Role", "PATIENT"))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.errorCode").value("USER_NOT_FOUND"));
		}
	}
}
