package com.todak_todag.user_service.user.application.service.query;

import com.todak_todag.user_service.global.exception.BusinessException;
import com.todak_todag.user_service.global.exception.ConsentDocumentErrorCode;
import com.todak_todag.user_service.user.application.result.ConsentDocumentFindDetailResult;
import com.todak_todag.user_service.user.application.result.ConsentDocumentFindResult;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentDetailView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ConsentDocumentQueryServiceTest {

    @Mock
    private ConsentDocumentQueryRepository consentDocumentQueryRepository;

    @InjectMocks
    private ConsentDocumentQueryService consentDocumentQueryService;

    @Nested
    @DisplayName("현재 적용 중인 약관 목록 조회")
    class FindCurrentConsentDocuments {

        @Test
        @DisplayName("현재 적용 중인 약관 목록을 조회한다")
        void findCurrentConsentDocuments_success() {
            // given
            UUID documentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();

            ConsentDocumentCurrentView view =
                    new ConsentDocumentCurrentView(
                            documentId,
                            versionId,
                            "TERMS_OF_SERVICE",
                            "서비스 이용약관",
                            "v2",
                            true
                    );

            given(consentDocumentQueryRepository.findAllCurrent(any(LocalDateTime.class)))
                    .willReturn(List.of(view));

            // when
            List<ConsentDocumentFindResult> results =
                    consentDocumentQueryService.findCurrentConsentDocuments();

            // then
            assertThat(results).hasSize(1);

            ConsentDocumentFindResult result = results.getFirst();

            assertThat(result.consentDocumentId()).isEqualTo(documentId);
            assertThat(result.consentDocumentVersionId()).isEqualTo(versionId);
            assertThat(result.consentType()).isEqualTo("TERMS_OF_SERVICE");
            assertThat(result.title()).isEqualTo("서비스 이용약관");
            assertThat(result.version()).isEqualTo("v2");
            assertThat(result.required()).isTrue();

            then(consentDocumentQueryRepository)
                    .should()
                    .findAllCurrent(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("현재 적용 중인 약관이 없으면 빈 목록을 반환한다")
        void findCurrentConsentDocuments_empty() {
            // given
            given(consentDocumentQueryRepository.findAllCurrent(any(LocalDateTime.class)))
                    .willReturn(List.of());

            // when
            List<ConsentDocumentFindResult> results =
                    consentDocumentQueryService.findCurrentConsentDocuments();

            // then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("약관 버전 ID로 현재 적용 약관 조회")
    class FindCurrentConsentDocumentsByVersionIds {

        @Test
        @DisplayName("중복된 약관 버전 ID를 제거한 뒤 조회한다")
        void findCurrentConsentDocumentsByVersionIds_success() {
            // given
            UUID versionId = UUID.randomUUID();

            ConsentDocumentCurrentView view =
                    new ConsentDocumentCurrentView(
                            UUID.randomUUID(),
                            versionId,
                            "PRIVACY_POLICY",
                            "개인정보 처리방침",
                            "v1",
                            true
                    );

            given(consentDocumentQueryRepository.findAllCurrentByVersionIds(
                    any(),
                    any(LocalDateTime.class)
            )).willReturn(List.of(view));

            // when
            List<ConsentDocumentFindResult> results =
                    consentDocumentQueryService.findCurrentConsentDocumentsByVersionIds(
                            List.of(versionId, versionId)
                    );

            // then
            assertThat(results).hasSize(1);

            ArgumentCaptor<List<UUID>> versionIdsCaptor =
                    ArgumentCaptor.forClass(List.class);

            then(consentDocumentQueryRepository)
                    .should()
                    .findAllCurrentByVersionIds(
                            versionIdsCaptor.capture(),
                            any(LocalDateTime.class)
                    );

            assertThat(versionIdsCaptor.getValue())
                    .containsExactly(versionId);
        }

        @Test
        @DisplayName("약관 버전 ID가 없으면 Repository를 호출하지 않고 빈 목록을 반환한다")
        void findCurrentConsentDocumentsByVersionIds_empty() {
            // when
            List<ConsentDocumentFindResult> results =
                    consentDocumentQueryService
                            .findCurrentConsentDocumentsByVersionIds(List.of());

            // then
            assertThat(results).isEmpty();

            then(consentDocumentQueryRepository)
                    .should(never())
                    .findAllCurrentByVersionIds(any(), any());
        }
    }

    @Nested
    @DisplayName("약관 상세 조회")
    class FindConsentDocumentDetail {

        @Test
        @DisplayName("약관 버전 ID로 상세 조회에 성공한다")
        void findConsentDocumentDetail_success() {
            // given
            UUID documentId = UUID.randomUUID();
            UUID versionId = UUID.randomUUID();
            LocalDateTime effectiveAt =
                    LocalDateTime.of(2026, 9, 1, 0, 0);

            ConsentDocumentDetailView view =
                    new ConsentDocumentDetailView(
                            documentId,
                            versionId,
                            "TERMS_OF_SERVICE",
                            "서비스 이용약관",
                            "v2",
                            "약관 본문입니다.",
                            true,
                            effectiveAt
                    );

            given(
                    consentDocumentQueryRepository.findDetailByVersionId(
                            versionId
                    )
            ).willReturn(Optional.of(view));

            // when
            ConsentDocumentFindDetailResult result =
                    consentDocumentQueryService
                            .findConsentDocumentDetail(versionId);

            // then
            assertThat(result.consentDocumentId())
                    .isEqualTo(documentId);
            assertThat(result.consentDocumentVersionId())
                    .isEqualTo(versionId);
            assertThat(result.consentType())
                    .isEqualTo("TERMS_OF_SERVICE");
            assertThat(result.title())
                    .isEqualTo("서비스 이용약관");
            assertThat(result.version())
                    .isEqualTo("v2");
            assertThat(result.content())
                    .isEqualTo("약관 본문입니다.");
            assertThat(result.required())
                    .isTrue();
            assertThat(result.effectiveAt())
                    .isEqualTo(effectiveAt);
        }

        @Test
        @DisplayName("존재하지 않는 약관 버전이면 예외가 발생한다")
        void findConsentDocumentDetail_notFound() {
            // given
            UUID versionId = UUID.randomUUID();

            given(
                    consentDocumentQueryRepository.findDetailByVersionId(
                            versionId
                    )
            ).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    consentDocumentQueryService
                            .findConsentDocumentDetail(versionId)
            )
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException =
                                (BusinessException) exception;

                        assertThat(businessException.getErrorCode())
                                .isEqualTo(
                                        ConsentDocumentErrorCode
                                                .CONSENT_DOCUMENT_NOT_FOUND
                                );
                    });
        }
    }
}