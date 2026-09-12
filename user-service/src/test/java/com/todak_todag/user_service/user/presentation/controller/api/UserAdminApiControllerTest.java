package com.todak_todag.user_service.user.presentation.controller.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.config.SecurityConfig;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserApprovalCommand;
import com.todak_todag.user_service.user.application.result.UserApprovalResult;
import com.todak_todag.user_service.user.application.service.command.UserCreateService;
import com.todak_todag.user_service.user.application.service.command.UserUpdateService;
import com.todak_todag.user_service.user.application.service.query.UserQueryService;

@WebMvcTest(UserAdminApiController.class)
@ImportAutoConfiguration(AopAutoConfiguration.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("UserAdminApiController 웹 단 테스트")
class UserAdminApiControllerTest {

	private static final UUID TARGET_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

	private static final UUID REQUESTER_ID = UUID.fromString("3b9a8f7c-1d2e-4a5b-9c8d-7e6f5a4b3c2d");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserCreateService userCreateService;

	@MockitoBean
	private UserUpdateService userUpdateService;

	@MockitoBean
	private UserQueryService userQueryService;

	@Nested
	@DisplayName("회원가입 승인/거절")
	class Approval {

		private static final String STATUS_URI = "/api/v1/admin/users/" + TARGET_ID + "/status";

		private static UserApprovalResult approvedResult() {
			return new UserApprovalResult(TARGET_ID, UserRole.HOSPITAL_STAFF, null, true);
		}

		private static UserApprovalResult rejectedResult(String rejectReason) {
			return new UserApprovalResult(TARGET_ID, UserRole.HOSPITAL_STAFF, rejectReason, false);
		}

		@Test
		@DisplayName("MASTER 가 승인 요청하면 200과 함께 승인 완료 메시지를 반환한다")
		void approvalTest_success_accept() throws Exception {
			// given
			given(userUpdateService.approval(any())).willReturn(approvedResult());

			// when & then
			mockMvc.perform(patch(STATUS_URI)
							.header("X-User-Id", REQUESTER_ID.toString())
							.header("X-User-Role", UserRole.MASTER.name())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "accept": true,
									  "rejectReason": null
									}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.success").value(true))
					.andExpect(jsonPath("$.message").value("회원가입 요청 승인 완료"))
					.andExpect(jsonPath("$.data.userId").value(TARGET_ID.toString()))
					.andExpect(jsonPath("$.data.role").value(UserRole.HOSPITAL_STAFF.getKoreaName()));
		}

		@Test
		@DisplayName("ADMIN 이 거절 요청하면 200과 함께 거절 완료 메시지와 사유를 반환한다")
		void approvalTest_success_reject() throws Exception {
			// given
			given(userUpdateService.approval(any())).willReturn(rejectedResult("제출 서류 미비"));

			// when & then
			mockMvc.perform(patch(STATUS_URI)
							.header("X-User-Id", REQUESTER_ID.toString())
							.header("X-User-Role", UserRole.ADMIN.name())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "accept": false,
									  "rejectReason": "제출 서류 미비"
									}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.message").value("회원가입 요청 거절 완료"))
					.andExpect(jsonPath("$.data.rejectReason").value("제출 서류 미비"));
		}

		@Test
		@DisplayName("경로의 userId 가 Command 에 담겨 서비스로 전달된다")
		void approvalTest_success_pathUserIdIsPassedToCommand() throws Exception {
			// given
			given(userUpdateService.approval(any())).willReturn(approvedResult());

			// when
			mockMvc.perform(patch(STATUS_URI)
							.header("X-User-Id", REQUESTER_ID.toString())
							.header("X-User-Role", UserRole.MASTER.name())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "accept": true
									}
									"""))
					.andExpect(status().isOk());

			// then
			ArgumentCaptor<UserApprovalCommand> captor = ArgumentCaptor.forClass(UserApprovalCommand.class);
			then(userUpdateService).should().approval(captor.capture());

			assertThat(captor.getValue().userId()).isEqualTo(TARGET_ID);
			assertThat(captor.getValue().requesterId()).isEqualTo(REQUESTER_ID);
			assertThat(captor.getValue().accept()).isTrue();
		}

		@Test
		@DisplayName("accept 가 없으면 400을 반환하고 서비스를 호출하지 않는다")
		void approvalTest_fail_acceptIsNull() throws Exception {
			// when & then
			mockMvc.perform(patch(STATUS_URI)
							.header("X-User-Id", REQUESTER_ID.toString())
							.header("X-User-Role", UserRole.MASTER.name())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "rejectReason": "제출 서류 미비"
									}
									"""))
					.andExpect(status().isBadRequest());

			then(userUpdateService).should(never()).approval(any());
		}

		@Test
		@DisplayName("userId 가 UUID 형식이 아니면 400을 반환하고 서비스를 호출하지 않는다")
		void approvalTest_fail_invalidUuid() throws Exception {
			// when & then
			mockMvc.perform(patch("/api/v1/admin/users/not-a-uuid/status")
							.header("X-User-Id", REQUESTER_ID.toString())
							.header("X-User-Role", UserRole.MASTER.name())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "accept": true
									}
									"""))
					.andExpect(status().isBadRequest());

			then(userUpdateService).should(never()).approval(any());
		}

		@Test
		@DisplayName("MASTER/ADMIN 이 아니면 403을 반환하고 서비스를 호출하지 않는다")
		void approvalTest_fail_forbiddenRole() throws Exception {
			// when & then
			mockMvc.perform(patch(STATUS_URI)
							.header("X-User-Id", REQUESTER_ID.toString())
							.header("X-User-Role", UserRole.HOSPITAL_STAFF.name())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "accept": true
									}
									"""))
					.andExpect(status().isForbidden());

			then(userUpdateService).should(never()).approval(any());
		}

		@Test
		@DisplayName("인증 헤더 없이 요청하면 인증에 실패하고 서비스를 호출하지 않는다")
		void approvalTest_fail_unauthenticated() throws Exception {
			// when & then
			mockMvc.perform(patch(STATUS_URI)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "accept": true
									}
									"""))
					.andExpect(status().is4xxClientError());

			then(userUpdateService).should(never()).approval(any());
		}

		@Test
		@DisplayName("승인 대상이 대기 상태가 아니면 409 에러 응답을 반환한다")
		void approvalTest_fail_invalidState() throws Exception {
			// given
			willThrow(new BusinessException(UserErrorCode.USER_MODIFY_STATE))
					.given(userUpdateService).approval(any());

			// when & then
			mockMvc.perform(patch(STATUS_URI)
							.header("X-User-Id", REQUESTER_ID.toString())
							.header("X-User-Role", UserRole.MASTER.name())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "accept": true
									}
									"""))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.success").value(false))
					.andExpect(jsonPath("$.error.errorCode").value("USER_MODIFY_STATE"));
		}
	}

	@Nested
	@DisplayName("사용자 일시 정지")
	class Suspend {

		private static final String SUSPEND_URI = "/api/v1/admin/users/" + TARGET_ID + "/suspend";

		@Test
		@DisplayName("MASTER 가 정지 요청하면 200과 함께 대상 식별자를 반환한다")
		void suspendTest_success() throws Exception {
			// given
			given(userUpdateService.suspend(any())).willReturn(TARGET_ID);

			// when & then
			mockMvc.perform(patch(SUSPEND_URI)
							.header("X-User-Id", REQUESTER_ID.toString())
							.header("X-User-Role", UserRole.MASTER.name())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "suspendReason": "약관 위반"
									}
									"""))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.message").value("해당 사용자가 일시 정지 되었습니다."))
					.andExpect(jsonPath("$.data.userId").value(TARGET_ID.toString()));
		}

		@Test
		@DisplayName("MASTER/ADMIN 이 아니면 403을 반환하고 서비스를 호출하지 않는다")
		void suspendTest_fail_forbiddenRole() throws Exception {
			// when & then
			mockMvc.perform(patch(SUSPEND_URI)
							.header("X-User-Id", REQUESTER_ID.toString())
							.header("X-User-Role", UserRole.PATIENT.name())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "suspendReason": "약관 위반"
									}
									"""))
					.andExpect(status().isForbidden());

			then(userUpdateService).should(never()).suspend(any());
		}

		@Test
		@DisplayName("정지 대상이 승인 상태가 아니면 409 에러 응답을 반환한다")
		void suspendTest_fail_invalidState() throws Exception {
			// given
			willThrow(new BusinessException(UserErrorCode.USER_SUSPEND_MODIFY_STATE))
					.given(userUpdateService).suspend(any());

			// when & then
			mockMvc.perform(patch(SUSPEND_URI)
							.header("X-User-Id", REQUESTER_ID.toString())
							.header("X-User-Role", UserRole.MASTER.name())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									  "suspendReason": "약관 위반"
									}
									"""))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.error.errorCode").value("USER_SUSPEND_MODIFY_STATE"));
		}
	}
}
