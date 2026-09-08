package com.todak_todag.user_service.user.application.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.todak_todag.user_service.global.common.UserRole;
import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentErrorCode;
import com.todak_todag.user_service.global.exception.UserErrorCode;
import com.todak_todag.user_service.user.application.command.UserSignupCommand;
import com.todak_todag.user_service.user.application.command.UserSignupCommand.AgreementCommand;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsentDocumentValidator 단위테스트")
class ConsentDocumentValidatorTest {

	private static final UUID REGION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	private static final UUID REQUIRED_VERSION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

	private static final UUID OPTIONAL_VERSION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	private static final UUID UNKNOWN_VERSION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

	@Mock
	private ConsentDocumentQueryRepository consentDocumentQueryRepo;

	@InjectMocks
	private ConsentDocumentValidator consentDocumentValidator;

	private static ConsentDocumentCurrentView currentView(UUID versionId, boolean required) {
		return new ConsentDocumentCurrentView(
				UUID.randomUUID(), versionId, "PERSONAL_INFORMATION", "약관", "v1", required
		);
	}

	private static UserSignupCommand signupCommand(List<AgreementCommand> agreements) {
		return new UserSignupCommand(
				UserRole.HOSPITAL_STAFF,
				"example0123",
				"Example0123@",
				"김영수",
				"01012345678",
				REGION_ID,
				agreements
		);
	}

	private void givenCurrentDocuments(ConsentDocumentCurrentView... views) {
		given(consentDocumentQueryRepo.findAllCurrent(any(LocalDateTime.class))).willReturn(List.of(views));
	}

	@Test
	@DisplayName("요청에 중복된 termsId가 있으면 DUPLICATE_CONSENT_DOCUMENT_VERSION 예외가 발생한다")
	void validateTest_fail_duplicateTermsId() {
		// Given
		UserSignupCommand command = signupCommand(List.of(
				new AgreementCommand(REQUIRED_VERSION_ID, true),
				new AgreementCommand(REQUIRED_VERSION_ID, false)
		));

		// When & Then
		assertThatThrownBy(() -> consentDocumentValidator.signupConsentDocumentValidate(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ConsentErrorCode.DUPLICATE_CONSENT_DOCUMENT_VERSION);
	}

	@Test
	@DisplayName("동의(agreed=true)한 termsId가 현재 유효한 버전이 아니면 INVALID_CONSENT_DOCUMENT_VERSION 예외가 발생한다")
	void validateTest_fail_invalidAgreedVersion() {
		// Given
		givenCurrentDocuments(currentView(REQUIRED_VERSION_ID, true));

		UserSignupCommand command = signupCommand(List.of(
				new AgreementCommand(UNKNOWN_VERSION_ID, true)
		));

		// When & Then
		assertThatThrownBy(() -> consentDocumentValidator.signupConsentDocumentValidate(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ConsentErrorCode.INVALID_CONSENT_DOCUMENT_VERSION);
	}

	@Test
	@DisplayName("현재 필수 약관 중 동의하지 않은 게 있으면 USER_SIGNUP_REQUIRED_NOT_AGREED 예외가 발생한다")
	void validateTest_fail_requiredNotAgreed() {
		// Given
		givenCurrentDocuments(
				currentView(REQUIRED_VERSION_ID, true),
				currentView(OPTIONAL_VERSION_ID, false)
		);

		// 필수 약관(REQUIRED_VERSION_ID)을 요청에서 아예 빼먹었다
		UserSignupCommand command = signupCommand(List.of(
				new AgreementCommand(OPTIONAL_VERSION_ID, true)
		));

		// When & Then
		assertThatThrownBy(() -> consentDocumentValidator.signupConsentDocumentValidate(command))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(UserErrorCode.USER_SIGNUP_REQUIRED_NOT_AGREED);
	}

	@Test
	@DisplayName("필수/선택 약관 모두 동의하면 동의한 버전 id 집합을 그대로 반환한다")
	void validateTest_success_allAgreed() {
		// Given
		givenCurrentDocuments(
				currentView(REQUIRED_VERSION_ID, true),
				currentView(OPTIONAL_VERSION_ID, false)
		);

		UserSignupCommand command = signupCommand(List.of(
				new AgreementCommand(REQUIRED_VERSION_ID, true),
				new AgreementCommand(OPTIONAL_VERSION_ID, true)
		));

		// When
		Set<UUID> result = consentDocumentValidator.signupConsentDocumentValidate(command);

		// Then
		assertThat(result).containsExactlyInAnyOrder(REQUIRED_VERSION_ID, OPTIONAL_VERSION_ID);
	}

	@Test
	@DisplayName("선택 약관을 거부(agreed=false)해도 필수 약관만 동의했으면 통과하고, 반환 집합에는 거부한 약관이 빠진다")
	void validateTest_success_optionalDeclined() {
		// Given
		givenCurrentDocuments(
				currentView(REQUIRED_VERSION_ID, true),
				currentView(OPTIONAL_VERSION_ID, false)
		);

		UserSignupCommand command = signupCommand(List.of(
				new AgreementCommand(REQUIRED_VERSION_ID, true),
				new AgreementCommand(OPTIONAL_VERSION_ID, false)
		));

		// When
		Set<UUID> result = consentDocumentValidator.signupConsentDocumentValidate(command);

		// Then
		assertThat(result).containsExactly(REQUIRED_VERSION_ID);
	}

	@Test
	@DisplayName("현재 유효하지 않은 termsId를 거부(agreed=false)로 보내면 유효성 검증 없이 무시되고 통과한다")
	void validateTest_success_declinedUnknownVersionIsIgnored() {
		// Given
		givenCurrentDocuments(currentView(REQUIRED_VERSION_ID, true));

		UserSignupCommand command = signupCommand(List.of(
				new AgreementCommand(REQUIRED_VERSION_ID, true),
				new AgreementCommand(UNKNOWN_VERSION_ID, false)
		));

		// When
		Set<UUID> result = consentDocumentValidator.signupConsentDocumentValidate(command);

		// Then
		assertThat(result).containsExactly(REQUIRED_VERSION_ID);
	}
}
