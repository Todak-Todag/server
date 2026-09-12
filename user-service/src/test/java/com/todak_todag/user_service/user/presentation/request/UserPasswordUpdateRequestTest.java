package com.todak_todag.user_service.user.presentation.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.security.UserContext;
import com.todak_todag.user_service.user.application.command.UserPasswordUpdateCommand;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class UserPasswordUpdateRequestTest {

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
	@DisplayName("영문/숫자/특수문자를 모두 포함한 8~20자 비밀번호는 검증을 통과한다")
	void validPassword() {
		UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPw123!", "newPw123!");

		Set<ConstraintViolation<UserPasswordUpdateRequest>> violations = validator.validate(request);

		assertThat(violations).isEmpty();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {" "})
	@DisplayName("기존 비밀번호가 없거나 공백이면 검증에 실패한다")
	void blankCurrentPassword(String currentPassword) {
		UserPasswordUpdateRequest request = new UserPasswordUpdateRequest(currentPassword, "newPw123!");

		Set<ConstraintViolation<UserPasswordUpdateRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(ConstraintViolation::getPropertyPath)
				.extracting(Object::toString)
				.contains("currentPassword");
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"Aa1!Aa1",       // 7자 - 8자 미만
			"Password!",     // 숫자 없음
			"12345678!",     // 영문 없음
			"Password123",   // 특수문자 없음
			"Pass word1!",   // 공백 포함
	})
	@DisplayName("영문/숫자/특수문자 조건 중 하나라도 어긋나면 검증에 실패한다")
	void invalidPasswordPattern(String newPassword) {
		UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPw123!", newPassword);

		Set<ConstraintViolation<UserPasswordUpdateRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(ConstraintViolation::getPropertyPath)
				.extracting(Object::toString)
				.contains("newPassword");
	}

	@Test
	@DisplayName("새 비밀번호가 20자를 초과하면 검증에 실패한다")
	void newPasswordTooLong() {
		String tooLong = "Aa1!Aa1!Aa1!Aa1!Aa1!X"; // 21자, 패턴 조건은 만족하지만 길이 초과
		UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPw123!", tooLong);

		Set<ConstraintViolation<UserPasswordUpdateRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(ConstraintViolation::getPropertyPath)
				.extracting(Object::toString)
				.contains("newPassword");
	}

	@ParameterizedTest
	@NullAndEmptySource
	@DisplayName("새 비밀번호가 없으면 검증에 실패한다")
	void blankNewPassword(String newPassword) {
		UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPw123!", newPassword);

		Set<ConstraintViolation<UserPasswordUpdateRequest>> violations = validator.validate(request);

		assertThat(violations)
				.extracting(ConstraintViolation::getPropertyPath)
				.extracting(Object::toString)
				.contains("newPassword");
	}

	@Test
	@DisplayName("toCommand()는 currentPassword/newPassword/accessToken/요청자를 그대로 Command에 담는다")
	void toCommand_mapsFieldsAsIs() {
		UserContext user = UserContext.from(UUID.randomUUID().toString(), UserRole.PATIENT.name());
		UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPw123!", "newPw123!");

		UserPasswordUpdateCommand command = request.toCommand("access-token-value", user);

		assertThat(command.currentPassword()).isEqualTo("currentPw123!");
		assertThat(command.newPassword()).isEqualTo("newPw123!");
		assertThat(command.accessToken()).isEqualTo("access-token-value");
		assertThat(command.requesterId()).isEqualTo(user.getUserId());
	}

	@Test
	@DisplayName("toCommand()는 쿠키가 없어 accessToken이 null이어도 그대로 Command에 담는다")
	void toCommand_nullAccessTokenIsPassedThrough() {
		UserContext user = UserContext.from(UUID.randomUUID().toString(), UserRole.PATIENT.name());
		UserPasswordUpdateRequest request = new UserPasswordUpdateRequest("currentPw123!", "newPw123!");

		UserPasswordUpdateCommand command = request.toCommand(null, user);

		assertThat(command.accessToken()).isNull();
	}
}
