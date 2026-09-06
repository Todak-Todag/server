package com.todak_todag.user_service.user.application.service.command;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentDocumentErrorCode;
import com.todak_todag.user_service.user.application.command.ConsentDocumentCreateCommand;
import com.todak_todag.user_service.user.application.command.ConsentDocumentDeleteCommand;
import com.todak_todag.user_service.user.application.command.ConsentDocumentUpdateRequiredCommand;
import com.todak_todag.user_service.user.application.result.ConsentDocumentCreateResult;
import com.todak_todag.user_service.user.application.result.ConsentDocumentUpdateRequiredResult;
import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;
import com.todak_todag.user_service.user.domain.repository.command.ConsentDocumentCommandRepository;
import com.todak_todag.user_service.user.domain.repository.command.ConsentDocumentVersionCommandRepository;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.todak_todag.user_service.user.domain.entity.ConsentDocument.ConsentType;
import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ConsentDocumentCommandServiceTest {

    @Mock
    private ConsentDocumentQueryRepository consentDocumentQueryRepository;

    @Mock
    private ConsentDocumentCommandRepository consentDocumentCommandRepository;

    @Mock
    private ConsentDocumentVersionCommandRepository
            consentDocumentVersionCommandRepository;

    @InjectMocks
    private ConsentDocumentCommandService consentDocumentCommandService;

    @Nested
    @DisplayName("약관 필수 여부 변경")
    class UpdateRequired {

        @Test
        @DisplayName("약관의 필수 여부 변경에 성공한다")
        void updateRequired_success() {
            // given
            UUID consentDocumentId = UUID.randomUUID();

            ConsentDocument consentDocument =
                    org.mockito.Mockito.mock(ConsentDocument.class);

            ConsentDocumentUpdateRequiredCommand command =
                    new ConsentDocumentUpdateRequiredCommand(
                            consentDocumentId,
                            false
                    );

            given(
                    consentDocumentQueryRepository.findById(
                            consentDocumentId
                    )
            ).willReturn(Optional.of(consentDocument));

            given(consentDocument.getId())
                    .willReturn(consentDocumentId);

            given(consentDocument.isRequired())
                    .willReturn(false);

            // when
            ConsentDocumentUpdateRequiredResult result =
                    consentDocumentCommandService.updateRequired(command);

            // then
            then(consentDocument)
                    .should()
                    .updateRequired(false);

            assertThat(result.consentDocumentId())
                    .isEqualTo(consentDocumentId);

            assertThat(result.required())
                    .isFalse();
        }

        @Test
        @DisplayName("존재하지 않는 약관이면 예외가 발생한다")
        void updateRequired_notFound() {
            // given
            UUID consentDocumentId = UUID.randomUUID();

            ConsentDocumentUpdateRequiredCommand command =
                    new ConsentDocumentUpdateRequiredCommand(
                            consentDocumentId,
                            false
                    );

            given(
                    consentDocumentQueryRepository.findById(
                            consentDocumentId
                    )
            ).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    consentDocumentCommandService.updateRequired(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(
                                businessException.getErrorCode()
                        ).isEqualTo(
                                ConsentDocumentErrorCode
                                        .CONSENT_DOCUMENT_NOT_FOUND
                        );
                    });
        }
    }

    @Nested
    @DisplayName("약관 사용 종료")
    class DeleteConsentDocument {

        @Test
        @DisplayName("약관 논리 삭제에 성공한다")
        void delete_success() {
            // given
            UUID consentDocumentId = UUID.randomUUID();
            UUID deletedBy = UUID.randomUUID();

            ConsentDocument consentDocument =
                    org.mockito.Mockito.mock(
                            ConsentDocument.class
                    );

            ConsentDocumentDeleteCommand command =
                    new ConsentDocumentDeleteCommand(
                            consentDocumentId,
                            deletedBy
                    );

            given(
                    consentDocumentQueryRepository
                            .findByIdIncludingDeleted(
                                    consentDocumentId
                            )
            ).willReturn(
                    Optional.of(consentDocument)
            );

            given(consentDocument.isDeleted())
                    .willReturn(false);

            given(consentDocument.getId())
                    .willReturn(consentDocumentId);

            // when
            consentDocumentCommandService.delete(command);

            // then
            then(consentDocument)
                    .should()
                    .markDeleted(deletedBy);
        }

        @Test
        @DisplayName("존재하지 않는 약관이면 예외가 발생한다")
        void delete_notFound() {
            // given
            UUID consentDocumentId = UUID.randomUUID();
            UUID deletedBy = UUID.randomUUID();

            ConsentDocumentDeleteCommand command =
                    new ConsentDocumentDeleteCommand(
                            consentDocumentId,
                            deletedBy
                    );

            given(
                    consentDocumentQueryRepository
                            .findByIdIncludingDeleted(
                                    consentDocumentId
                            )
            ).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    consentDocumentCommandService.delete(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(
                                businessException.getErrorCode()
                        ).isEqualTo(
                                ConsentDocumentErrorCode
                                        .CONSENT_DOCUMENT_NOT_FOUND
                        );
                    });
        }

        @Test
        @DisplayName("이미 사용 종료된 약관이면 예외가 발생한다")
        void delete_alreadyDeleted() {
            // given
            UUID consentDocumentId = UUID.randomUUID();
            UUID deletedBy = UUID.randomUUID();

            ConsentDocument consentDocument =
                    org.mockito.Mockito.mock(
                            ConsentDocument.class
                    );

            ConsentDocumentDeleteCommand command =
                    new ConsentDocumentDeleteCommand(
                            consentDocumentId,
                            deletedBy
                    );

            given(
                    consentDocumentQueryRepository
                            .findByIdIncludingDeleted(
                                    consentDocumentId
                            )
            ).willReturn(
                    Optional.of(consentDocument)
            );

            given(consentDocument.isDeleted())
                    .willReturn(true);

            // when & then
            assertThatThrownBy(() ->
                    consentDocumentCommandService.delete(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(
                                businessException.getErrorCode()
                        ).isEqualTo(
                                ConsentDocumentErrorCode
                                        .CONSENT_DOCUMENT_ALREADY_DELETED
                        );
                    });

            then(consentDocument)
                    .shouldHaveNoMoreInteractions();
        }
    }
    @Nested
    @DisplayName("신규 약관 및 최초 버전 등록")
    class CreateConsentDocument {

        @Test
        @DisplayName("신규 약관과 최초 버전 등록에 성공한다")
        void create_success() {
            // given
            UUID consentDocumentId = UUID.randomUUID();
            UUID consentDocumentVersionId = UUID.randomUUID();

            LocalDateTime effectiveAt =
                    LocalDateTime.of(2026, 9, 10, 0, 0);

            ConsentDocumentCreateCommand command =
                    new ConsentDocumentCreateCommand(
                            ConsentType.PERSONAL_INFORMATION,
                            "개인정보 수집 및 이용 동의",
                            true,
                            "1.0",
                            "개인정보 수집 및 이용 약관 내용입니다.",
                            effectiveAt
                    );

            ConsentDocument savedDocument =
                    org.mockito.Mockito.mock(
                            ConsentDocument.class
                    );

            ConsentDocumentVersion savedVersion =
                    org.mockito.Mockito.mock(
                            ConsentDocumentVersion.class
                    );

            given(
                    consentDocumentQueryRepository.existsByConsentType(
                            ConsentType.PERSONAL_INFORMATION
                    )
            ).willReturn(false);

            given(
                    consentDocumentCommandRepository.save(
                            any(ConsentDocument.class)
                    )
            ).willReturn(savedDocument);

            given(savedDocument.getId())
                    .willReturn(consentDocumentId);

            given(savedDocument.getConsentType())
                    .willReturn(
                            ConsentType.PERSONAL_INFORMATION
                    );

            given(
                    consentDocumentVersionCommandRepository.save(
                            any(ConsentDocumentVersion.class)
                    )
            ).willReturn(savedVersion);

            given(savedVersion.getId())
                    .willReturn(consentDocumentVersionId);

            // when
            ConsentDocumentCreateResult result =
                    consentDocumentCommandService.create(command);

            // then
            assertThat(result.consentDocumentId())
                    .isEqualTo(consentDocumentId);

            assertThat(result.consentDocumentVersionId())
                    .isEqualTo(consentDocumentVersionId);

            then(consentDocumentCommandRepository)
                    .should()
                    .save(any(ConsentDocument.class));

            then(consentDocumentVersionCommandRepository)
                    .should()
                    .save(any(ConsentDocumentVersion.class));
        }

        @Test
        @DisplayName("동일 유형의 약관이 이미 존재하면 예외가 발생한다")
        void create_alreadyExists() {
            // given
            ConsentDocumentCreateCommand command =
                    new ConsentDocumentCreateCommand(
                            ConsentType.PERSONAL_INFORMATION,
                            "개인정보 수집 및 이용 동의",
                            true,
                            "1.0",
                            "개인정보 수집 및 이용 약관 내용입니다.",
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    10,
                                    0,
                                    0
                            )
                    );

            given(
                    consentDocumentQueryRepository.existsByConsentType(
                            ConsentDocument.ConsentType.PERSONAL_INFORMATION
                    )
            ).willReturn(true);

            // when & then
            assertThatThrownBy(() ->
                    consentDocumentCommandService.create(command)
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(
                                businessException.getErrorCode()
                        ).isEqualTo(
                                ConsentDocumentErrorCode
                                        .CONSENT_DOCUMENT_ALREADY_EXISTS
                        );
                    });

            then(consentDocumentCommandRepository)
                    .shouldHaveNoInteractions();

            then(consentDocumentVersionCommandRepository)
                    .shouldHaveNoInteractions();
        }
    }
}