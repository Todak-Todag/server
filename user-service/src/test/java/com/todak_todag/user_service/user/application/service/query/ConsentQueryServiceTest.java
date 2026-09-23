package com.todak_todag.user_service.user.application.service.query;

import com.todak_todag.user_service.user.application.result.ConsentFindHistoryResult;
import com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus;
import com.todak_todag.user_service.user.domain.repository.query.ConsentHistoryView;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ConsentQueryServiceTest {

    @Mock
    private ConsentQueryRepository consentQueryRepository;

    @InjectMocks
    private ConsentQueryService consentQueryService;

    @Nested
    @DisplayName("로그인 사용자 약관 동의 내역 조회")
    class FindMyConsents {

        @Test
        @DisplayName("사용자의 동의 및 철회 내역을 조회한다")
        void findMyConsents_success() {

            // given
            UUID userId = UUID.randomUUID();

            UUID consentId1 = UUID.randomUUID();
            UUID consentDocumentId1 = UUID.randomUUID();
            UUID consentDocumentVersionId1 =
                    UUID.randomUUID();

            UUID consentId2 = UUID.randomUUID();
            UUID consentDocumentId2 = UUID.randomUUID();
            UUID consentDocumentVersionId2 =
                    UUID.randomUUID();

            LocalDateTime agreedAt1 =
                    LocalDateTime.of(
                            2026,
                            9,
                            1,
                            10,
                            30
                    );

            LocalDateTime agreedAt2 =
                    LocalDateTime.of(
                            2026,
                            8,
                            20,
                            14,
                            0
                    );

            LocalDateTime withdrawnAt2 =
                    LocalDateTime.of(
                            2026,
                            8,
                            25,
                            9,
                            30
                    );

            ConsentHistoryView agreedConsent =
                    new ConsentHistoryView(
                            consentId1,
                            consentDocumentId1,
                            consentDocumentVersionId1,
                            "개인정보 수집 및 이용 동의",
                            "1.0",
                            ConsentStatus.AGREED,
                            agreedAt1,
                            null
                    );

            ConsentHistoryView withdrawnConsent =
                    new ConsentHistoryView(
                            consentId2,
                            consentDocumentId2,
                            consentDocumentVersionId2,
                            "마케팅 정보 수신 동의",
                            "1.0",
                            ConsentStatus.WITHDRAWN,
                            agreedAt2,
                            withdrawnAt2
                    );

            given(
                    consentQueryRepository
                            .findAllByUserId(userId)
            ).willReturn(
                    List.of(
                            agreedConsent,
                            withdrawnConsent
                    )
            );

            // when
            List<ConsentFindHistoryResult> results =
                    consentQueryService.findMyConsents(
                            userId
                    );

            // then
            assertThat(results)
                    .hasSize(2);

            assertThat(results.get(0).consentId())
                    .isEqualTo(consentId1);

            assertThat(results.get(0).status())
                    .isEqualTo(ConsentStatus.AGREED);

            assertThat(results.get(0).withdrawnAt())
                    .isNull();

            assertThat(results.get(1).consentId())
                    .isEqualTo(consentId2);

            assertThat(results.get(1).status())
                    .isEqualTo(
                            ConsentStatus.WITHDRAWN
                    );

            assertThat(results.get(1).withdrawnAt())
                    .isEqualTo(withdrawnAt2);

            then(consentQueryRepository)
                    .should()
                    .findAllByUserId(userId);
        }

        @Test
        @DisplayName("동의 내역이 없으면 빈 목록을 반환한다")
        void findMyConsents_empty() {

            // given
            UUID userId = UUID.randomUUID();

            given(
                    consentQueryRepository
                            .findAllByUserId(userId)
            ).willReturn(List.of());

            // when
            List<ConsentFindHistoryResult> results =
                    consentQueryService.findMyConsents(
                            userId
                    );

            // then
            assertThat(results)
                    .isEmpty();

            then(consentQueryRepository)
                    .should()
                    .findAllByUserId(userId);
        }
    }
}