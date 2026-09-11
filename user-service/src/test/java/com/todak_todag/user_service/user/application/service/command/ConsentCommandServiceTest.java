package com.todak_todag.user_service.user.application.service.command;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentErrorCode;
import com.todak_todag.user_service.user.application.command.ConsentCreateCommand;
import com.todak_todag.user_service.user.application.command.ConsentWithdrawCommand;
import com.todak_todag.user_service.user.application.result.ConsentCreateResult;
import com.todak_todag.user_service.user.application.result.ConsentWithdrawResult;
import com.todak_todag.user_service.user.domain.entity.Consent;
import com.todak_todag.user_service.user.domain.entity.user.User;
import com.todak_todag.user_service.user.domain.entity.user.UserStatus;
import com.todak_todag.user_service.user.domain.repository.command.ConsentCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.ConsentQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.UserQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ConsentCommandServiceTest {

    @Mock
    private ConsentDocumentQueryRepository
            consentDocumentQueryRepository;

    @Mock
    private ConsentQueryRepository consentQueryRepository;

    @Mock
    private ConsentCommandRepository consentCommandRepository;

    @Mock
    private UserQueryRepository userQueryRepository;

    @InjectMocks
    private ConsentCommandService consentCommandService;

    @Nested
    @DisplayName("약관 동의")
    class CreateConsent {

        @Test
        @DisplayName("필수 약관을 모두 동의하지 않은 환자는 WITHDRAWN 상태를 유지한다")
        void create_requiredConsentNotCompleted_patientRemainsWithdrawn() {
            // given
            UUID userId = UUID.randomUUID();

            UUID requestedVersionId = UUID.randomUUID();
            UUID otherRequiredVersionId = UUID.randomUUID();

            UUID consentId = UUID.randomUUID();

            ConsentCreateCommand command =
                    new ConsentCreateCommand(
                            userId,
                            List.of(requestedVersionId)
                    );

            ConsentDocumentCurrentView requestedVersion =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            requestedVersionId,
                            "PERSONAL_INFORMATION",
                            "개인정보 수집 및 이용 동의",
                            "1.0",
                            true
                    );

            ConsentDocumentCurrentView otherRequiredVersion =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            otherRequiredVersionId,
                            "SENSITIVE_INFORMATION",
                            "민감정보 수집 및 이용 동의",
                            "1.0",
                            true
                    );

            User patient = User.createPatient(
                    UUID.randomUUID(),
                    "patient",
                    "passwordHash",
                    "환자",
                    "010-1111-2222",
                    "서울시 테스트 주소"
            );

            given(
                    consentDocumentQueryRepository
                            .findAllCurrentByVersionIds(
                                    anyList(),
                                    any()
                            )
            ).willReturn(
                    List.of(requestedVersion)
            );

            given(
                    consentQueryRepository.existsAgreedConsent(
                            userId,
                            List.of(requestedVersionId)
                    )
            ).willReturn(false);

            Consent savedConsent =
                    org.mockito.Mockito.mock(
                            Consent.class
                    );

            given(savedConsent.getId())
                    .willReturn(consentId);

            given(
                    consentCommandRepository.saveAll(
                            anyList()
                    )
            ).willReturn(
                    List.of(savedConsent)
            );

            given(
                    userQueryRepository.findById(userId)
            ).willReturn(
                    Optional.of(patient)
            );

            // 현재 적용 중인 필수 약관은 총 2개
            given(
                    consentDocumentQueryRepository
                            .findAllCurrent(any())
            ).willReturn(
                    List.of(
                            requestedVersion,
                            otherRequiredVersion
                    )
            );

            // 사용자가 AGREED 상태로 가지고 있는 필수 약관은 1개
            given(
                    consentQueryRepository.countAgreedConsents(
                            userId,
                            List.of(
                                    requestedVersionId,
                                    otherRequiredVersionId
                            )
                    )
            ).willReturn(1L);

            // when
            consentCommandService.create(command);

            // then
            assertThat(patient.getStatus())
                    .isEqualTo(UserStatus.WITHDRAWN);

            then(consentQueryRepository)
                    .should()
                    .countAgreedConsents(
                            userId,
                            List.of(
                                    requestedVersionId,
                                    otherRequiredVersionId
                            )
                    );
        }

        @Test
        @DisplayName("현재 필수 약관을 모두 동의한 WITHDRAWN 환자는 APPROVED 상태가 된다")
        void create_allRequiredConsentCompleted_withdrawnPatientApproved() {
            // given
            UUID userId = UUID.randomUUID();

            UUID versionId1 = UUID.randomUUID();
            UUID versionId2 = UUID.randomUUID();

            UUID consentId = UUID.randomUUID();

            // 이번 요청에서는 마지막 남은 필수 약관에 동의
            ConsentCreateCommand command =
                    new ConsentCreateCommand(
                            userId,
                            List.of(versionId2)
                    );

            ConsentDocumentCurrentView requiredVersion1 =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            versionId1,
                            "PERSONAL_INFORMATION",
                            "개인정보 수집 및 이용 동의",
                            "1.0",
                            true
                    );

            ConsentDocumentCurrentView requiredVersion2 =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            versionId2,
                            "SENSITIVE_INFORMATION",
                            "민감정보 수집 및 이용 동의",
                            "1.0",
                            true
                    );

            User patient = User.createPatient(
                    UUID.randomUUID(),
                    "patient",
                    "passwordHash",
                    "환자",
                    "010-1111-2222",
                    "서울시 테스트 주소"
            );

            assertThat(patient.getStatus())
                    .isEqualTo(UserStatus.WITHDRAWN);

            // 이번 요청에 포함된 versionId2가 현재 적용 버전인지 검증
            given(
                    consentDocumentQueryRepository
                            .findAllCurrentByVersionIds(
                                    anyList(),
                                    any()
                            )
            ).willReturn(
                    List.of(requiredVersion2)
            );

            given(
                    consentQueryRepository.existsAgreedConsent(
                            userId,
                            List.of(versionId2)
                    )
            ).willReturn(false);

            Consent savedConsent =
                    org.mockito.Mockito.mock(
                            Consent.class
                    );

            given(savedConsent.getId())
                    .willReturn(consentId);

            given(
                    consentCommandRepository.saveAll(
                            anyList()
                    )
            ).willReturn(
                    List.of(savedConsent)
            );

            given(
                    userQueryRepository.findById(userId)
            ).willReturn(
                    Optional.of(patient)
            );

            // 현재 필수 약관 전체
            given(
                    consentDocumentQueryRepository
                            .findAllCurrent(any())
            ).willReturn(
                    List.of(
                            requiredVersion1,
                            requiredVersion2
                    )
            );

            // 기존 1개 + 이번에 저장한 1개 = 모두 AGREED
            given(
                    consentQueryRepository.countAgreedConsents(
                            userId,
                            List.of(
                                    versionId1,
                                    versionId2
                            )
                    )
            ).willReturn(2L);

            // when
            consentCommandService.create(command);

            // then
            assertThat(patient.getStatus())
                    .isEqualTo(UserStatus.APPROVED);

            then(userQueryRepository)
                    .should()
                    .findById(userId);

            then(consentQueryRepository)
                    .should()
                    .countAgreedConsents(
                            userId,
                            List.of(
                                    versionId1,
                                    versionId2
                            )
                    );
        }

        @Test
        @DisplayName("이미 APPROVED 상태인 환자는 필수 약관 완료 여부를 다시 검사하지 않는다")
        void create_alreadyApprovedPatient_doesNotChangeStatus() {
            // given
            UUID userId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();
            UUID consentId = UUID.randomUUID();

            ConsentCreateCommand command =
                    new ConsentCreateCommand(
                            userId,
                            List.of(versionId)
                    );

            ConsentDocumentCurrentView currentVersion =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            versionId,
                            "MARKETING_INFORMATION",
                            "마케팅 정보 수신 동의",
                            "1.0",
                            false
                    );

            given(
                    consentDocumentQueryRepository
                            .findAllCurrentByVersionIds(
                                    anyList(),
                                    any()
                            )
            ).willReturn(
                    List.of(currentVersion)
            );

            given(
                    consentQueryRepository.existsAgreedConsent(
                            userId,
                            List.of(versionId)
                    )
            ).willReturn(false);

            Consent savedConsent =
                    org.mockito.Mockito.mock(
                            Consent.class
                    );

            given(savedConsent.getId())
                    .willReturn(consentId);

            given(
                    consentCommandRepository.saveAll(
                            anyList()
                    )
            ).willReturn(
                    List.of(savedConsent)
            );

            User user =
                    org.mockito.Mockito.mock(User.class);

            given(user.isPatient())
                    .willReturn(true);

            given(user.isWithdrawn())
                    .willReturn(false);

            given(
                    userQueryRepository.findById(userId)
            ).willReturn(
                    Optional.of(user)
            );

            // when
            consentCommandService.create(command);

            // then
            then(consentDocumentQueryRepository)
                    .shouldHaveNoMoreInteractions();

            then(consentQueryRepository)
                    .shouldHaveNoMoreInteractions();
        }

        @Test
        @DisplayName("선택 약관에 동의하지 않아도 필수 약관을 모두 동의하면 APPROVED 상태가 된다")
        void create_optionalConsentNotAgreed_requiredCompleted_patientApproved() {
            // given
            UUID userId = UUID.randomUUID();

            UUID requiredVersionId1 = UUID.randomUUID();
            UUID requiredVersionId2 = UUID.randomUUID();
            UUID optionalVersionId = UUID.randomUUID();

            UUID consentId = UUID.randomUUID();

            ConsentCreateCommand command =
                    new ConsentCreateCommand(
                            userId,
                            List.of(requiredVersionId2)
                    );

            ConsentDocumentCurrentView requiredVersion1 =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            requiredVersionId1,
                            "PERSONAL_INFORMATION",
                            "개인정보 수집 및 이용 동의",
                            "1.0",
                            true
                    );

            ConsentDocumentCurrentView requiredVersion2 =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            requiredVersionId2,
                            "SENSITIVE_INFORMATION",
                            "민감정보 수집 및 이용 동의",
                            "1.0",
                            true
                    );

            ConsentDocumentCurrentView optionalVersion =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            optionalVersionId,
                            "MARKETING_INFORMATION",
                            "마케팅 정보 수신 동의",
                            "1.0",
                            false
                    );

            User patient = User.createPatient(
                    UUID.randomUUID(),
                    "patient",
                    "passwordHash",
                    "환자",
                    "010-1111-2222",
                    "서울시 테스트 주소"
            );

            given(
                    consentDocumentQueryRepository
                            .findAllCurrentByVersionIds(
                                    anyList(),
                                    any()
                            )
            ).willReturn(
                    List.of(requiredVersion2)
            );

            given(
                    consentQueryRepository.existsAgreedConsent(
                            userId,
                            List.of(requiredVersionId2)
                    )
            ).willReturn(false);

            Consent savedConsent =
                    org.mockito.Mockito.mock(
                            Consent.class
                    );

            given(savedConsent.getId())
                    .willReturn(consentId);

            given(
                    consentCommandRepository.saveAll(anyList())
            ).willReturn(
                    List.of(savedConsent)
            );

            given(
                    userQueryRepository.findById(userId)
            ).willReturn(
                    Optional.of(patient)
            );

            // 현재 약관에는 선택 약관도 포함되어 있음
            given(
                    consentDocumentQueryRepository
                            .findAllCurrent(any())
            ).willReturn(
                    List.of(
                            requiredVersion1,
                            requiredVersion2,
                            optionalVersion
                    )
            );

            // count 대상은 필수 약관 두 개뿐
            given(
                    consentQueryRepository.countAgreedConsents(
                            userId,
                            List.of(
                                    requiredVersionId1,
                                    requiredVersionId2
                            )
                    )
            ).willReturn(2L);

            // when
            consentCommandService.create(command);

            // then
            assertThat(patient.getStatus())
                    .isEqualTo(UserStatus.APPROVED);

            then(consentQueryRepository)
                    .should()
                    .countAgreedConsents(
                            userId,
                            List.of(
                                    requiredVersionId1,
                                    requiredVersionId2
                            )
                    );
        }

        @Test
        @DisplayName("현재 적용 중인 여러 약관 버전에 동의한다")
        void create_success() {
            // given
            UUID userId = UUID.randomUUID();

            UUID versionId1 = UUID.randomUUID();
            UUID versionId2 = UUID.randomUUID();

            UUID consentId1 = UUID.randomUUID();
            UUID consentId2 = UUID.randomUUID();

            ConsentCreateCommand command =
                    new ConsentCreateCommand(
                            userId,
                            List.of(
                                    versionId1,
                                    versionId2
                            )
                    );

            ConsentDocumentCurrentView currentVersion1 =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            versionId1,
                            "PERSONAL_INFORMATION",
                            "개인정보 수집 및 이용 동의",
                            "1.0",
                            true
                    );

            ConsentDocumentCurrentView currentVersion2 =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            versionId2,
                            "MARKETING_INFORMATION",
                            "마케팅 정보 수신 동의",
                            "1.0",
                            false
                    );

            given(
                    consentDocumentQueryRepository
                            .findAllCurrentByVersionIds(
                                    anyList(),
                                    any()
                            )
            ).willReturn(
                    List.of(
                            currentVersion1,
                            currentVersion2
                    )
            );

            given(
                    consentQueryRepository.existsAgreedConsent(
                            userId,
                            List.of(
                                    versionId1,
                                    versionId2
                            )
                    )
            ).willReturn(false);

            Consent savedConsent1 =
                    org.mockito.Mockito.mock(
                            Consent.class
                    );

            Consent savedConsent2 =
                    org.mockito.Mockito.mock(
                            Consent.class
                    );

            given(savedConsent1.getId())
                    .willReturn(consentId1);

            given(savedConsent2.getId())
                    .willReturn(consentId2);

            given(
                    consentCommandRepository.saveAll(
                            anyList()
                    )
            ).willReturn(
                    List.of(
                            savedConsent1,
                            savedConsent2
                    )
            );

            // 일반적인 약관 동의 성공 검증이 목적이므로
// 환자 재활성화 로직은 진행하지 않도록 설정
            User user =
                    org.mockito.Mockito.mock(User.class);

            given(
                    userQueryRepository.findById(userId)
            ).willReturn(
                    Optional.of(user)
            );

            given(user.isPatient())
                    .willReturn(false);

            // when
            ConsentCreateResult result =
                    consentCommandService.create(command);

            // then
            assertThat(result.consentIds())
                    .containsExactly(
                            consentId1,
                            consentId2
                    );

            then(consentDocumentQueryRepository)
                    .should()
                    .findAllCurrentByVersionIds(
                            anyList(),
                            any()
                    );

            then(consentQueryRepository)
                    .should()
                    .existsAgreedConsent(
                            userId,
                            List.of(
                                    versionId1,
                                    versionId2
                            )
                    );

            then(consentCommandRepository)
                    .should()
                    .saveAll(anyList());
        }

        @Test
        @DisplayName("요청에 동일한 약관 버전이 중복되면 예외가 발생한다")
        void create_duplicateVersionIds() {
            // given
            UUID userId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            ConsentCreateCommand command =
                    new ConsentCreateCommand(
                            userId,
                            List.of(
                                    versionId,
                                    versionId
                            )
                    );

            // when & then
            assertThatThrownBy(
                    () -> consentCommandService.create(
                            command
                    )
            )
                    .isInstanceOf(
                            BusinessException.class
                    )
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(
                                businessException.getErrorCode()
                        ).isEqualTo(
                                ConsentErrorCode
                                        .DUPLICATE_CONSENT_DOCUMENT_VERSION
                        );
                    });

            then(consentDocumentQueryRepository)
                    .shouldHaveNoInteractions();

            then(consentQueryRepository)
                    .shouldHaveNoInteractions();

            then(consentCommandRepository)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("현재 동의할 수 없는 약관 버전이 포함되면 예외가 발생한다")
        void create_invalidConsentDocumentVersion() {
            // given
            UUID userId = UUID.randomUUID();

            UUID currentVersionId =
                    UUID.randomUUID();

            UUID invalidVersionId =
                    UUID.randomUUID();

            ConsentCreateCommand command =
                    new ConsentCreateCommand(
                            userId,
                            List.of(
                                    currentVersionId,
                                    invalidVersionId
                            )
                    );

            ConsentDocumentCurrentView currentVersion =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            currentVersionId,
                            "PERSONAL_INFORMATION",
                            "개인정보 수집 및 이용 동의",
                            "1.0",
                            true
                    );

            // 요청은 2개지만 현재 동의 가능한 버전은 1개만 조회됨
            given(
                    consentDocumentQueryRepository
                            .findAllCurrentByVersionIds(
                                    anyList(),
                                    any()
                            )
            ).willReturn(
                    List.of(currentVersion)
            );

            // when & then
            assertThatThrownBy(
                    () -> consentCommandService.create(
                            command
                    )
            )
                    .isInstanceOf(
                            BusinessException.class
                    )
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(
                                businessException.getErrorCode()
                        ).isEqualTo(
                                ConsentErrorCode
                                        .INVALID_CONSENT_DOCUMENT_VERSION
                        );
                    });

            then(consentQueryRepository)
                    .shouldHaveNoInteractions();

            then(consentCommandRepository)
                    .shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이미 동의한 약관이 포함되면 예외가 발생한다")
        void create_alreadyAgreed() {
            // given
            UUID userId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            ConsentCreateCommand command =
                    new ConsentCreateCommand(
                            userId,
                            List.of(versionId)
                    );

            ConsentDocumentCurrentView currentVersion =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            versionId,
                            "PERSONAL_INFORMATION",
                            "개인정보 수집 및 이용 동의",
                            "1.0",
                            true
                    );

            given(
                    consentDocumentQueryRepository
                            .findAllCurrentByVersionIds(
                                    anyList(),
                                    any()
                            )
            ).willReturn(
                    List.of(currentVersion)
            );

            given(
                    consentQueryRepository.existsAgreedConsent(
                            userId,
                            List.of(versionId)
                    )
            ).willReturn(true);

            // when & then
            assertThatThrownBy(
                    () -> consentCommandService.create(
                            command
                    )
            )
                    .isInstanceOf(
                            BusinessException.class
                    )
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(
                                businessException.getErrorCode()
                        ).isEqualTo(
                                ConsentErrorCode
                                        .CONSENT_ALREADY_AGREED
                        );
                    });

            then(consentCommandRepository)
                    .shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("약관 동의 철회")
    class WithdrawConsent {

        @Test
        @DisplayName("본인의 동의 내역을 철회한다")
        void withdraw_success() {
            // given
            UUID userId = UUID.randomUUID();
            UUID consentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            ConsentWithdrawCommand command =
                    new ConsentWithdrawCommand(
                            userId,
                            consentId
                    );

            Consent consent =
                    Consent.agree(
                            userId,
                            versionId,
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    1,
                                    10,
                                    30
                            )
                    );

            given(
                    consentCommandRepository.findById(
                            consentId
                    )
            ).willReturn(
                    Optional.of(consent)
            );

            // 철회할 약관이 선택 약관이라고 설정
            given(
                    consentDocumentQueryRepository.findRequiredByVersionId(
                            versionId
                    )
            ).willReturn(
                    Optional.of(false)
            );

            // when
            ConsentWithdrawResult result =
                    consentCommandService.withdraw(
                            command
                    );

            // then
            assertThat(result.consentId())
                    .isEqualTo(consentId);

            assertThat(result.status())
                    .isEqualTo(
                            Consent.ConsentStatus.WITHDRAWN
                    );

            assertThat(result.withdrawnAt())
                    .isNotNull();

            assertThat(consent.getStatus())
                    .isEqualTo(
                            Consent.ConsentStatus.WITHDRAWN
                    );

            assertThat(consent.getWithdrawnAt())
                    .isNotNull();

            then(consentCommandRepository)
                    .should()
                    .findById(consentId);
        }

        @Test
        @DisplayName("동의 내역이 존재하지 않으면 예외가 발생한다")
        void withdraw_notFound() {
            // given
            UUID userId = UUID.randomUUID();
            UUID consentId = UUID.randomUUID();

            ConsentWithdrawCommand command =
                    new ConsentWithdrawCommand(
                            userId,
                            consentId
                    );

            given(
                    consentCommandRepository.findById(
                            consentId
                    )
            ).willReturn(
                    Optional.empty()
            );

            // when & then
            assertThatThrownBy(
                    () -> consentCommandService.withdraw(
                            command
                    )
            )
                    .isInstanceOf(
                            BusinessException.class
                    )
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(
                                businessException.getErrorCode()
                        ).isEqualTo(
                                ConsentErrorCode
                                        .CONSENT_NOT_FOUND
                        );
                    });

            then(consentCommandRepository)
                    .should()
                    .findById(consentId);
        }

        @Test
        @DisplayName("다른 사용자의 동의 내역은 철회할 수 없다")
        void withdraw_accessDenied() {
            // given
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();
            UUID consentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            ConsentWithdrawCommand command =
                    new ConsentWithdrawCommand(
                            userId,
                            consentId
                    );

            Consent consent =
                    Consent.agree(
                            otherUserId,
                            versionId,
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    1,
                                    10,
                                    30
                            )
                    );

            given(
                    consentCommandRepository.findById(
                            consentId
                    )
            ).willReturn(
                    Optional.of(consent)
            );

            // when & then
            assertThatThrownBy(
                    () -> consentCommandService.withdraw(
                            command
                    )
            )
                    .isInstanceOf(
                            BusinessException.class
                    )
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(
                                businessException.getErrorCode()
                        ).isEqualTo(
                                ConsentErrorCode
                                        .CONSENT_ACCESS_DENIED
                        );
                    });

            // 권한이 없는 사용자의 요청이므로 상태는 변경되지 않는다.
            assertThat(consent.getStatus())
                    .isEqualTo(
                            Consent.ConsentStatus.AGREED
                    );

            assertThat(consent.getWithdrawnAt())
                    .isNull();
        }

        @Test
        @DisplayName("이미 철회된 동의 내역은 다시 철회할 수 없다")
        void withdraw_alreadyWithdrawn() {
            // given
            UUID userId = UUID.randomUUID();
            UUID consentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            ConsentWithdrawCommand command =
                    new ConsentWithdrawCommand(
                            userId,
                            consentId
                    );

            Consent consent =
                    Consent.agree(
                            userId,
                            versionId,
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    1,
                                    10,
                                    30
                            )
                    );

            consent.withdraw(
                    LocalDateTime.of(
                            2026,
                            9,
                            2,
                            10,
                            30
                    )
            );

            given(
                    consentCommandRepository.findById(
                            consentId
                    )
            ).willReturn(
                    Optional.of(consent)
            );

            // when & then
            assertThatThrownBy(
                    () -> consentCommandService.withdraw(
                            command
                    )
            )
                    .isInstanceOf(
                            BusinessException.class
                    )
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(
                                businessException.getErrorCode()
                        ).isEqualTo(
                                ConsentErrorCode
                                        .CONSENT_ALREADY_WITHDRAWN
                        );
                    });
        }
    }

    @Nested
    @DisplayName("약관 철회에 따른 사용자 상태 변경")
    class WithdrawUserStatus {

        @Test
        @DisplayName("필수 약관을 철회하면 APPROVED 사용자가 WITHDRAWN으로 변경된다")
        void withdraw_requiredConsent_changesUserToWithdrawn() {
            // given
            User user = createApprovedPatient();
            UUID consentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            Consent consent = stubConsent(user, consentId, versionId, true);

            given(userQueryRepository.findById(user.getId()))
                    .willReturn(Optional.of(user));

            // when
            ConsentWithdrawResult result = consentCommandService.withdraw(
                    new ConsentWithdrawCommand(user.getId(), consentId)
            );

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertWithdrawn(consent, result, consentId);
        }

        @Test
        @DisplayName("선택 약관을 철회하면 APPROVED 사용자 상태가 유지된다")
        void withdraw_optionalConsent_keepsApproved() {
            // given
            User user = createApprovedPatient();
            UUID consentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            Consent consent = stubConsent(user, consentId, versionId, false);

            // when
            ConsentWithdrawResult result = consentCommandService.withdraw(
                    new ConsentWithdrawCommand(user.getId(), consentId)
            );

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.APPROVED);
            assertWithdrawn(consent, result, consentId);

            // 선택 약관은 사용자 상태 변경을 위한 조회 자체가 필요 없다.
            then(userQueryRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("이미 WITHDRAWN인 사용자가 선택 약관을 철회해도 승인 상태로 복구되지 않는다")
        void withdraw_optionalConsent_keepsWithdrawn() {
            // given: 환자는 생성 시 WITHDRAWN 상태다.
            User user = createPatient();
            UUID consentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            Consent consent = stubConsent(user, consentId, versionId, false);

            // when
            ConsentWithdrawResult result = consentCommandService.withdraw(
                    new ConsentWithdrawCommand(user.getId(), consentId)
            );

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.WITHDRAWN);
            assertWithdrawn(consent, result, consentId);

            then(userQueryRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("정지된 사용자가 필수 약관을 철회해도 SUSPENDED 상태가 유지된다")
        void withdraw_requiredConsent_keepsSuspended() {
            // given
            User user = createApprovedPatient();
            user.suspend("운영 정책 위반");

            UUID consentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            Consent consent = stubConsent(user, consentId, versionId, true);

            given(userQueryRepository.findById(user.getId()))
                    .willReturn(Optional.of(user));

            // when
            ConsentWithdrawResult result = consentCommandService.withdraw(
                    new ConsentWithdrawCommand(user.getId(), consentId)
            );

            // then
            assertThat(user.getStatus()).isEqualTo(UserStatus.SUSPENDED);
            assertThat(user.getStatusChangeReason()).isEqualTo("운영 정책 위반");
            assertWithdrawn(consent, result, consentId);
        }

        @Test
        @DisplayName("약관 버전을 찾을 수 없으면 동의 내역을 변경하지 않는다")
        void withdraw_versionNotFound() {
            // given
            User user = createApprovedPatient();
            UUID consentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            Consent consent = Consent.agree(
                    user.getId(),
                    versionId,
                    LocalDateTime.now().minusDays(1)
            );

            given(consentCommandRepository.findById(consentId))
                    .willReturn(Optional.of(consent));

            given(consentDocumentQueryRepository.findRequiredByVersionId(versionId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> consentCommandService.withdraw(
                    new ConsentWithdrawCommand(user.getId(), consentId)
            ))
                    .isInstanceOfSatisfying(
                            BusinessException.class,
                            exception -> assertThat(exception.getErrorCode())
                                    .isEqualTo(
                                            ConsentErrorCode.INVALID_CONSENT_DOCUMENT_VERSION
                                    )
                    );

            assertThat(consent.getStatus())
                    .isEqualTo(Consent.ConsentStatus.AGREED);
            assertThat(consent.getWithdrawnAt()).isNull();
            assertThat(user.getStatus()).isEqualTo(UserStatus.APPROVED);

            then(userQueryRepository).shouldHaveNoInteractions();
        }

        private User createPatient() {
            return User.createPatient(
                    UUID.randomUUID(),
                    "patient",
                    "passwordHash",
                    "테스트 환자",
                    "010-1111-2222",
                    "서울시 테스트 주소"
            );
        }

        private User createApprovedPatient() {
            User user = createPatient();
            user.approveFromRequiredConsent();
            return user;
        }

        private Consent stubConsent(
                User user,
                UUID consentId,
                UUID versionId,
                boolean required
        ) {
            Consent consent = Consent.agree(
                    user.getId(),
                    versionId,
                    LocalDateTime.now().minusDays(1)
            );

            given(consentCommandRepository.findById(consentId))
                    .willReturn(Optional.of(consent));

            given(consentDocumentQueryRepository.findRequiredByVersionId(versionId))
                    .willReturn(Optional.of(required));

            return consent;
        }

        private void assertWithdrawn(
                Consent consent,
                ConsentWithdrawResult result,
                UUID consentId
        ) {
            assertThat(consent.getStatus())
                    .isEqualTo(Consent.ConsentStatus.WITHDRAWN);
            assertThat(consent.getWithdrawnAt()).isNotNull();

            assertThat(result.consentId()).isEqualTo(consentId);
            assertThat(result.status())
                    .isEqualTo(Consent.ConsentStatus.WITHDRAWN);
            assertThat(result.withdrawnAt()).isEqualTo(consent.getWithdrawnAt());
        }
    }
}