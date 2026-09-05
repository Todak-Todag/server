package com.todak_todag.user_service.user.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.CommonErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserApprovalCommand;
import com.todak_todag.user_service.user.application.command.UserSuspendCommand;
import com.todak_todag.user_service.user.application.result.UserApprovalResult;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;
import com.todak_todag.user_service.user.domain.repository.command.UserCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserUpdateService 단위테스트")
class UserUpdateServiceTest {

	private static final UUID TARGET_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

	private static final UUID ADMIN_ID = UUID.fromString("3b9a8f7c-1d2e-4a5b-9c8d-7e6f5a4b3c2d");

	private static final UUID MASTER_ID = UUID.fromString("e012a1b2-c3d4-4e5f-8a9b-0c1d2e3f4a5b");

	private static final UUID REGION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private static final UUID OTHER_REGION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private static final String REJECT_REASON = "제출 서류 미비";

	@Mock
	private UserCommandRepository userCommandRepo;

	@Mock
	private UserQueryRepository userQueryRepo;

	@InjectMocks
	private UserUpdateService userUpdateService;

	// 승인 대기중인 대상 유저를 흉내낸다
	private static User pendingTarget(UUID regionId) {
		User user = User.createSignup(
				regionId,
				"target0123",
				"$2a$10$hashedvaluehashedvaluehashedvalue",
				"김환자",
				"01012345678",
				UserRole.HOSPITAL_STAFF
		);
		ReflectionTestUtils.setField(user, "id", TARGET_ID);
		return user;
	}

	// 요청자가 운영자일 때 사용할 운영자 유저를 흉내낸다
	private static User admin(UUID regionId) {
		User user = User.createAdmin(
				regionId,
				"admin0123",
				"$2a$10$hashedvaluehashedvaluehashedvalue",
				"관리자",
				"01099998888",
				UserRole.ADMIN
		);
		ReflectionTestUtils.setField(user, "id", ADMIN_ID);
		return user;
	}

	private static UserApprovalCommand approvalCommand(
			Boolean accept,
			String rejectReason,
			UUID requesterId,
			UserRole requesterRole
	) {
		UserContext requester = UserContext.from(requesterId.toString(), requesterRole.name());
		return new UserApprovalCommand(TARGET_ID, accept, rejectReason, requester);
	}

	// 정지 대상으로 사용할 승인 완료 상태의 유저를 흉내낸다
	private static User approvedTarget(UUID regionId) {
		User user = User.createSignup(
				regionId,
				"target0123",
				"$2a$10$hashedvaluehashedvaluehashedvalue",
				"김환자",
				"01012345678",
				UserRole.HOSPITAL_STAFF
		);
		user.approvalOrReject(true, null);
		ReflectionTestUtils.setField(user, "id", TARGET_ID);
		return user;
	}

	// 정지 대상이 운영자인 경우를 흉내낸다
	private static User adminTarget(UUID regionId) {
		User user = User.createAdmin(
				regionId,
				"admintarget0123",
				"$2a$10$hashedvaluehashedvaluehashedvalue",
				"운영자대상",
				"01088887777",
				UserRole.ADMIN
		);
		ReflectionTestUtils.setField(user, "id", TARGET_ID);
		return user;
	}

	private static UserSuspendCommand suspendCommand(
			String suspendReason,
			UUID requesterId,
			UserRole requesterRole
	) {
		UserContext requester = UserContext.from(requesterId.toString(), requesterRole.name());
		return new UserSuspendCommand(TARGET_ID, suspendReason, requester);
	}

	@Nested
	@DisplayName("대상 사용자 조회")
	class FindTargetUser {

		@Test
		@DisplayName("대상 사용자가 존재하지 않으면 USER_NOT_FOUND 예외가 발생하고 이후 처리를 하지 않는다")
		void approvalTest_fail_targetNotFound() {
			// Given
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.empty());
			UserApprovalCommand command = approvalCommand(true, null, MASTER_ID, UserRole.MASTER);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.approval(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_NOT_FOUND);

			verify(userQueryRepo, never()).findAdminById(any());
		}
	}

	@Nested
	@DisplayName("운영자 요청 시 권한 검증")
	class AdminAuthorization {

		@Test
		@DisplayName("요청자가 운영자인데 존재하지 않는 관리자면 FORBIDDEN 예외가 발생하고 상태를 변경하지 않는다")
		void approvalTest_fail_adminNotFound() {
			// Given
			User target = pendingTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));
			given(userQueryRepo.findAdminById(ADMIN_ID)).willReturn(Optional.empty());

			UserApprovalCommand command = approvalCommand(true, null, ADMIN_ID, UserRole.ADMIN);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.approval(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(CommonErrorCode.FORBIDDEN);

			assertThat(target.getStatus()).isEqualTo(UserStatus.PENDING);
		}

		@Test
		@DisplayName("요청자가 대상과 다른 지역의 운영자면 FORBIDDEN 예외가 발생하고 상태를 변경하지 않는다")
		void approvalTest_fail_differentRegion() {
			// Given
			User target = pendingTarget(REGION_ID);
			User adminUser = admin(OTHER_REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));
			given(userQueryRepo.findAdminById(ADMIN_ID)).willReturn(Optional.of(adminUser));

			UserApprovalCommand command = approvalCommand(true, null, ADMIN_ID, UserRole.ADMIN);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.approval(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(CommonErrorCode.FORBIDDEN);

			assertThat(target.getStatus()).isEqualTo(UserStatus.PENDING);
		}

		@Test
		@DisplayName("요청자가 대상과 같은 지역의 운영자면 정상적으로 승인 처리된다")
		void approvalTest_success_sameRegionAdmin() {
			// Given
			User target = pendingTarget(REGION_ID);
			User adminUser = admin(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));
			given(userQueryRepo.findAdminById(ADMIN_ID)).willReturn(Optional.of(adminUser));

			UserApprovalCommand command = approvalCommand(true, null, ADMIN_ID, UserRole.ADMIN);

			// When
			UserApprovalResult result = userUpdateService.approval(command);

			// Then
			assertThat(target.getStatus()).isEqualTo(UserStatus.APPROVED);
			assertThat(result.isAccept()).isTrue();
		}

		@Test
		@DisplayName("요청자가 운영자가 아니면 관리자 조회 없이 바로 처리된다")
		void approvalTest_success_nonAdminRequesterSkipsAdminLookup() {
			// Given
			User target = pendingTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserApprovalCommand command = approvalCommand(true, null, MASTER_ID, UserRole.MASTER);

			// When
			userUpdateService.approval(command);

			// Then
			verify(userQueryRepo, never()).findAdminById(any());
			assertThat(target.getStatus()).isEqualTo(UserStatus.APPROVED);
		}
	}

	@Nested
	@DisplayName("승인")
	class Approve {

		@Test
		@DisplayName("대기중인 사용자를 승인하면 상태가 APPROVED 로 변경되고 사유가 초기화된다")
		void approvalTest_success() {
			// Given
			User target = pendingTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserApprovalCommand command = approvalCommand(true, null, MASTER_ID, UserRole.MASTER);

			// When
			UserApprovalResult result = userUpdateService.approval(command);

			// Then
			assertThat(target.getStatus()).isEqualTo(UserStatus.APPROVED);
			assertThat(target.getStatusChangeReason()).isNull();

			assertThat(result.userId()).isEqualTo(TARGET_ID);
			assertThat(result.role()).isEqualTo(UserRole.HOSPITAL_STAFF);
			assertThat(result.rejectReason()).isNull();
			assertThat(result.isAccept()).isTrue();
		}

		@Test
		@DisplayName("승인이면서 거절 사유가 함께 오면 USER_APPROVAL_CONFLICT 예외가 발생하고 상태를 변경하지 않는다")
		void approvalTest_fail_conflictWithRejectReason() {
			// Given
			User target = pendingTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserApprovalCommand command = approvalCommand(true, REJECT_REASON, MASTER_ID, UserRole.MASTER);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.approval(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_APPROVAL_CONFLICT);

			assertThat(target.getStatus()).isEqualTo(UserStatus.PENDING);
		}

		@Test
		@DisplayName("대기중이 아닌 사용자를 승인하려 하면 USER_MODIFY_STATE 예외가 발생한다")
		void approvalTest_fail_notPending() {
			// Given
			User target = pendingTarget(REGION_ID);
			target.approvalOrReject(true, null); // 이미 승인 완료된 상태로 전이시킨다

			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserApprovalCommand command = approvalCommand(true, null, MASTER_ID, UserRole.MASTER);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.approval(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_MODIFY_STATE);
		}
	}

	@Nested
	@DisplayName("거절")
	class Reject {

		@Test
		@DisplayName("대기중인 사용자를 거절하면 상태가 REJECTED 로 변경되고 사유가 저장된다")
		void approvalTest_success() {
			// Given
			User target = pendingTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserApprovalCommand command = approvalCommand(false, REJECT_REASON, MASTER_ID, UserRole.MASTER);

			// When
			UserApprovalResult result = userUpdateService.approval(command);

			// Then
			assertThat(target.getStatus()).isEqualTo(UserStatus.REJECTED);
			assertThat(target.getStatusChangeReason()).isEqualTo(REJECT_REASON);

			assertThat(result.userId()).isEqualTo(TARGET_ID);
			assertThat(result.rejectReason()).isEqualTo(REJECT_REASON);
			assertThat(result.isAccept()).isFalse();
		}

		@ParameterizedTest
		@NullAndEmptySource
		@ValueSource(strings = {" "})
		@DisplayName("거절 사유가 없거나 공백이면 USER_REJECT_CONFLICT 예외가 발생하고 상태를 변경하지 않는다")
		void approvalTest_fail_blankRejectReason(String rejectReason) {
			// Given
			User target = pendingTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserApprovalCommand command = approvalCommand(false, rejectReason, MASTER_ID, UserRole.MASTER);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.approval(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_REJECT_CONFLICT);

			assertThat(target.getStatus()).isEqualTo(UserStatus.PENDING);
		}

		@Test
		@DisplayName("대기중이 아닌 사용자를 거절하려 하면 USER_MODIFY_STATE 예외가 발생한다")
		void approvalTest_fail_notPending() {
			// Given
			User target = pendingTarget(REGION_ID);
			target.approvalOrReject(true, null); // 이미 승인 완료된 상태로 전이시킨다

			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserApprovalCommand command = approvalCommand(false, REJECT_REASON, MASTER_ID, UserRole.MASTER);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.approval(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_MODIFY_STATE);
		}
	}

	@Nested
	@DisplayName("일시 정지")
	class Suspend {

		@Test
		@DisplayName("MASTER 요청자가 승인된 사용자를 정지시키면 대상 식별자를 반환한다")
		void suspendTest_success_master() {
			// Given
			User target = approvedTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserSuspendCommand command = suspendCommand("약관 위반", MASTER_ID, UserRole.MASTER);

			// When
			UUID result = userUpdateService.suspend(command);

			// Then
			assertThat(result).isEqualTo(TARGET_ID);
			verify(userQueryRepo, never()).findActiveById(any());
		}

		@Test
		@DisplayName("정지 대상이 존재하지 않으면 USER_NOT_FOUND 예외가 발생한다")
		void suspendTest_fail_targetNotFound() {
			// Given
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.empty());

			UserSuspendCommand command = suspendCommand("약관 위반", MASTER_ID, UserRole.MASTER);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.suspend(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_NOT_FOUND);

			verify(userQueryRepo, never()).findActiveById(any());
		}

		@Test
		@DisplayName("대상이 승인(APPROVED) 상태가 아니면 USER_SUSPEND_MODIFY_STATE 예외가 발생한다")
		void suspendTest_fail_notApproved() {
			// Given
			User target = pendingTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserSuspendCommand command = suspendCommand("약관 위반", MASTER_ID, UserRole.MASTER);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.suspend(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(UserErrorCode.USER_SUSPEND_MODIFY_STATE);
		}

		@Test
		@DisplayName("요청자가 ADMIN이고 대상도 ADMIN이면 UNAUTHORIZED_INTERNAL_REQUEST 예외가 발생한다")
		void suspendTest_fail_targetIsAdmin() {
			// Given
			User target = adminTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserSuspendCommand command = suspendCommand("약관 위반", ADMIN_ID, UserRole.ADMIN);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.suspend(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);

			verify(userQueryRepo, never()).findActiveById(any());
		}

		@Test
		@DisplayName("요청자가 ADMIN인데 활성 상태로 조회되지 않으면 UNAUTHORIZED_INTERNAL_REQUEST 예외가 발생한다")
		void suspendTest_fail_requesterAdminNotFound() {
			// Given
			User target = approvedTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));
			given(userQueryRepo.findActiveById(ADMIN_ID)).willReturn(Optional.empty());

			UserSuspendCommand command = suspendCommand("약관 위반", ADMIN_ID, UserRole.ADMIN);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.suspend(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);
		}

		@Test
		@DisplayName("요청자가 대상과 같은 지역의 ADMIN이면 정상적으로 정지 처리된다")
		void suspendTest_success_sameRegionAdmin() {
			// Given
			User target = approvedTarget(REGION_ID);
			User adminUser = admin(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));
			given(userQueryRepo.findActiveById(ADMIN_ID)).willReturn(Optional.of(adminUser));

			UserSuspendCommand command = suspendCommand("약관 위반", ADMIN_ID, UserRole.ADMIN);

			// When
			UUID result = userUpdateService.suspend(command);

			// Then
			assertThat(result).isEqualTo(TARGET_ID);
		}

		@Test
		@DisplayName("요청자가 대상과 다른 지역의 ADMIN이면 UNAUTHORIZED_INTERNAL_REQUEST 예외가 발생한다")
		void suspendTest_fail_differentRegionAdmin() {
			// Given
			User target = approvedTarget(REGION_ID);
			User adminUser = admin(OTHER_REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));
			given(userQueryRepo.findActiveById(ADMIN_ID)).willReturn(Optional.of(adminUser));

			UserSuspendCommand command = suspendCommand("약관 위반", ADMIN_ID, UserRole.ADMIN);

			// When & Then
			assertThatThrownBy(() -> userUpdateService.suspend(command))
					.isInstanceOf(BusinessException.class)
					.extracting(e -> ((BusinessException) e).getErrorCode())
					.isEqualTo(CommonErrorCode.UNAUTHORIZED_INTERNAL_REQUEST);
		}

		@Test
		@DisplayName("정지 사유가 대상 User에 그대로 반영된다")
		void suspendTest_success_reasonIsSaved() {
			// Given
			User target = approvedTarget(REGION_ID);
			given(userQueryRepo.findById(TARGET_ID)).willReturn(Optional.of(target));

			UserSuspendCommand command = suspendCommand("약관 위반", MASTER_ID, UserRole.MASTER);

			// When
			userUpdateService.suspend(command);

			// Then
			assertThat(target.getStatusChangeReason()).isEqualTo("약관 위반");
		}
	}
}
