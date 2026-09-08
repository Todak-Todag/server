package com.todak_todag.user_service.user.application.service.command;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentErrorCode;
import com.todak_todag.user_service.user.application.command.ConsentCreateCommand;
import com.todak_todag.user_service.user.application.command.ConsentWithdrawCommand;
import com.todak_todag.user_service.user.application.result.ConsentCreateResult;
import com.todak_todag.user_service.user.application.result.ConsentWithdrawResult;
import com.todak_todag.user_service.user.domain.entity.Consent;
import com.todak_todag.user_service.user.domain.repository.command.ConsentCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import com.todak_todag.user_service.user.domain.repository.query.ConsentQueryRepository;
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

    @InjectMocks
    private ConsentCommandService consentCommandService;

    @Nested
    @DisplayName("약관 동의")
    class CreateConsent {

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
}