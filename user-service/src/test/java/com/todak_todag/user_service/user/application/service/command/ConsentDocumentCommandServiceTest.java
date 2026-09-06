package com.todak_todag.user_service.user.application.service.command;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentDocumentErrorCode;
import com.todak_todag.user_service.user.application.command.ConsentDocumentUpdateRequiredCommand;
import com.todak_todag.user_service.user.application.result.ConsentDocumentUpdateRequiredResult;
import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}