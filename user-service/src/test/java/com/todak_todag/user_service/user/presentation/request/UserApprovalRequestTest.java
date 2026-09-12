package com.todak_todag.user_service.user.presentation.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserApprovalCommand;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UserApprovalRequestTest {

	private static final UUID TARGET_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");

	private static ValidatorFactory validatorFactory;

	private static Validator validator;

	@BeforeAll
	static void setUp() {
		validatorFactory = Validation.buildDefaultValidatorFactory();
		validator = validatorFactory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		validatorFactory.close();
	}

	@Test
	@DisplayName("accept 가 true 이고 거절 사유가 없으면 검증을 통과한다")
	void validAccept() {
		UserApprovalRequest request = new UserApprovalRequest(true, null);

		Set<ConstraintViolation<UserApprovalRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("accept 가 false 이고 거절 사유가 있으면 검증을 통과한다")
	void validReject() {
		UserApprovalRequest request = new UserApprovalRequest(false, "제출 서류 미비");

		Set<ConstraintViolation<UserApprovalRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@Test
	@DisplayName("accept 가 null 이면 검증에 실패한다")
	void invalidWhenAcceptIsNull() {
		UserApprovalRequest request = new UserApprovalRequest(null, null);

		Set<ConstraintViolation<UserApprovalRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(ConstraintViolation::getPropertyPath)
				.extracting(Object::toString)
				.contains("accept");
	}

	@Test
	@DisplayName("toCommand()는 경로에서 받은 userId 와 accept/rejectReason/요청자를 그대로 Command 에 담는다")
	void toCommand_mapsPathUserIdAndFields() {
		UserContext requester = UserContext.from(UUID.randomUUID().toString(), UserRole.ADMIN.name());
		UserApprovalRequest request = new UserApprovalRequest(false, "제출 서류 미비");

		UserApprovalCommand command = request.toCommand(TARGET_ID, requester);

		assertThat(command.userId()).isEqualTo(TARGET_ID);
		assertThat(command.accept()).isFalse();
		assertThat(command.rejectReason()).isEqualTo("제출 서류 미비");
		assertThat(command.requesterId()).isEqualTo(requester.getUserId());
		assertThat(command.requesterRole()).isEqualTo(UserRole.ADMIN);
	}

	@Test
	@DisplayName("toCommand()의 userId 는 요청 본문이 아니라 경로 변수에서만 결정된다")
	void toCommand_userIdComesOnlyFromPath() {
		UserContext requester = UserContext.from(UUID.randomUUID().toString(), UserRole.MASTER.name());
		UserApprovalRequest request = new UserApprovalRequest(true, null);

		UUID otherId = UUID.fromString("11111111-1111-1111-1111-111111111111");

		assertThat(request.toCommand(TARGET_ID, requester).userId()).isEqualTo(TARGET_ID);
		assertThat(request.toCommand(otherId, requester).userId()).isEqualTo(otherId);
	}
}
