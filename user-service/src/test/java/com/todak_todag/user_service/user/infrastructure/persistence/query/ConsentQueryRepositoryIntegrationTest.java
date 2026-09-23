package com.todak_todag.user_service.user.infrastructure.persistence.query;

import com.todak_todag.user_service.support.PostgresTestSupport;
import com.todak_todag.user_service.user.domain.entity.Consent;
import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;
import com.todak_todag.user_service.user.domain.repository.query.ConsentHistoryView;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentVersionRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsentQueryRepositoryIntegrationTest
        extends PostgresTestSupport {

    @Autowired
    private ConsentQueryRepositoryImpl repository;

    @Autowired
    private JpaConsentRepository consentRepository;

    @Autowired
    private JpaConsentDocumentRepository documentRepository;

    @Autowired
    private JpaConsentDocumentVersionRepository versionRepository;

    @BeforeEach
    void setUp() {
        consentRepository.deleteAll();
        versionRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Nested
    @DisplayName("사용자 동의 내역 조회")
    class FindConsentHistory {

        @Test
        @DisplayName("사용자의 약관 동의 내역을 최신 동의순으로 조회한다")
        void findAllByUserId() {

            // given
            UUID userId = UUID.randomUUID();

            ConsentDocument personalDocument =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.PERSONAL_INFORMATION,
                            "개인정보 수집 및 이용 동의",
                            true
                    );

            ConsentDocument marketingDocument =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.MARKETING_INFORMATION,
                            "마케팅 정보 수신 동의",
                            false
                    );

            documentRepository.saveAll(
                    List.of(
                            personalDocument,
                            marketingDocument
                    )
            );

            documentRepository.flush();

            ConsentDocumentVersion personalVersion =
                    ConsentDocumentVersion.create(
                            personalDocument.getId(),
                            "1.0",
                            "개인정보 약관",
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    1,
                                    0,
                                    0
                            )
                    );

            ConsentDocumentVersion marketingVersion =
                    ConsentDocumentVersion.create(
                            marketingDocument.getId(),
                            "1.0",
                            "마케팅 약관",
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    1,
                                    0,
                                    0
                            )
                    );

            versionRepository.saveAll(
                    List.of(
                            personalVersion,
                            marketingVersion
                    )
            );

            versionRepository.flush();

            Consent oldConsent =
                    Consent.agree(
                            userId,
                            personalVersion.getId(),
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    2,
                                    10,
                                    0
                            )
                    );

            Consent latestConsent =
                    Consent.agree(
                            userId,
                            marketingVersion.getId(),
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    5,
                                    10,
                                    0
                            )
                    );

            consentRepository.saveAll(
                    List.of(
                            oldConsent,
                            latestConsent
                    )
            );

            consentRepository.flush();

            // when
            List<ConsentHistoryView> result =
                    repository.findAllByUserId(userId);

            // then
            assertThat(result)
                    .hasSize(2);

            assertThat(result.get(0).consentId())
                    .isEqualTo(latestConsent.getId());

            assertThat(result.get(0).title())
                    .isEqualTo("마케팅 정보 수신 동의");

            assertThat(result.get(1).consentId())
                    .isEqualTo(oldConsent.getId());

            assertThat(result.get(1).title())
                    .isEqualTo("개인정보 수집 및 이용 동의");
        }

        @Test
        @DisplayName("다른 사용자의 동의 내역은 조회하지 않는다")
        void excludeOtherUserConsent() {

            // given
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            ConsentDocument document =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.SENSITIVE_INFORMATION,
                            "민감정보 수집 및 이용 동의",
                            true
                    );

            documentRepository.saveAndFlush(document);

            ConsentDocumentVersion version =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "1.0",
                            "민감정보 약관",
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    1,
                                    0,
                                    0
                            )
                    );

            versionRepository.saveAndFlush(version);

            Consent userConsent =
                    Consent.agree(
                            userId,
                            version.getId(),
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    2,
                                    10,
                                    0
                            )
                    );

            Consent otherConsent =
                    Consent.agree(
                            otherUserId,
                            version.getId(),
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    2,
                                    11,
                                    0
                            )
                    );

            consentRepository.saveAll(
                    List.of(
                            userConsent,
                            otherConsent
                    )
            );

            consentRepository.flush();

            // when
            List<ConsentHistoryView> result =
                    repository.findAllByUserId(userId);

            // then
            assertThat(result)
                    .hasSize(1);

            assertThat(result.getFirst().consentId())
                    .isEqualTo(userConsent.getId());
        }

        @Test
        @DisplayName("철회된 동의도 사용자 동의 내역에 포함된다")
        void includeWithdrawnConsent() {

            // given
            UUID userId = UUID.randomUUID();

            ConsentDocument document =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.PERSONAL_INFORMATION,
                            "개인정보 수집 및 이용 동의",
                            true
                    );

            documentRepository.saveAndFlush(document);

            ConsentDocumentVersion version =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "1.0",
                            "개인정보 약관",
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    1,
                                    0,
                                    0
                            )
                    );

            versionRepository.saveAndFlush(version);

            LocalDateTime agreedAt =
                    LocalDateTime.of(
                            2026,
                            9,
                            2,
                            10,
                            0
                    );

            LocalDateTime withdrawnAt =
                    LocalDateTime.of(
                            2026,
                            9,
                            5,
                            18,
                            0
                    );

            Consent consent =
                    Consent.agree(
                            userId,
                            version.getId(),
                            agreedAt
                    );

            consent.withdraw(withdrawnAt);

            consentRepository.saveAndFlush(consent);

            // when
            List<ConsentHistoryView> result =
                    repository.findAllByUserId(userId);

            // then
            assertThat(result)
                    .hasSize(1);

            ConsentHistoryView history =
                    result.getFirst();

            assertThat(history.status())
                    .isEqualTo(
                            Consent.ConsentStatus.WITHDRAWN
                    );

            assertThat(history.agreedAt())
                    .isEqualTo(agreedAt);

            assertThat(history.withdrawnAt())
                    .isEqualTo(withdrawnAt);
        }
    }

    @Nested
    @DisplayName("유효한 동의 여부 확인")
    class ExistsAgreedConsent {

        @Test
        @DisplayName("AGREED 상태의 동의가 존재하면 true를 반환한다")
        void agreedConsentExists() {

            // given
            UUID userId = UUID.randomUUID();

            ConsentDocument document =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.PERSONAL_INFORMATION,
                            "개인정보 수집 및 이용 동의",
                            true
                    );

            documentRepository.saveAndFlush(document);

            ConsentDocumentVersion version =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "1.0",
                            "개인정보 약관",
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    1,
                                    0,
                                    0
                            )
                    );

            versionRepository.saveAndFlush(version);

            Consent consent =
                    Consent.agree(
                            userId,
                            version.getId(),
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    2,
                                    10,
                                    0
                            )
                    );

            consentRepository.saveAndFlush(consent);

            // when
            boolean result =
                    repository.existsAgreedConsent(
                            userId,
                            List.of(version.getId())
                    );

            // then
            assertThat(result)
                    .isTrue();
        }

        @Test
        @DisplayName("철회된 동의만 존재하면 유효한 동의로 판단하지 않는다")
        void withdrawnConsentDoesNotExistAsAgreed() {

            // given
            UUID userId = UUID.randomUUID();

            ConsentDocument document =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.PERSONAL_INFORMATION,
                            "개인정보 수집 및 이용 동의",
                            true
                    );

            documentRepository.saveAndFlush(document);

            ConsentDocumentVersion version =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "1.0",
                            "개인정보 약관",
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    1,
                                    0,
                                    0
                            )
                    );

            versionRepository.saveAndFlush(version);

            Consent consent =
                    Consent.agree(
                            userId,
                            version.getId(),
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    2,
                                    10,
                                    0
                            )
                    );

            consent.withdraw(
                    LocalDateTime.of(
                            2026,
                            9,
                            5,
                            18,
                            0
                    )
            );

            consentRepository.saveAndFlush(consent);

            // when
            boolean result =
                    repository.existsAgreedConsent(
                            userId,
                            List.of(version.getId())
                    );

            // then
            assertThat(result)
                    .isFalse();
        }

        @Test
        @DisplayName("다른 사용자의 동의는 현재 사용자의 유효한 동의로 판단하지 않는다")
        void otherUserConsentDoesNotExist() {

            // given
            UUID userId = UUID.randomUUID();
            UUID otherUserId = UUID.randomUUID();

            ConsentDocument document =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.PERSONAL_INFORMATION,
                            "개인정보 수집 및 이용 동의",
                            true
                    );

            documentRepository.saveAndFlush(document);

            ConsentDocumentVersion version =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "1.0",
                            "개인정보 약관",
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    1,
                                    0,
                                    0
                            )
                    );

            versionRepository.saveAndFlush(version);

            Consent otherUserConsent =
                    Consent.agree(
                            otherUserId,
                            version.getId(),
                            LocalDateTime.of(
                                    2026,
                                    9,
                                    2,
                                    10,
                                    0
                            )
                    );

            consentRepository.saveAndFlush(otherUserConsent);

            // when
            boolean result =
                    repository.existsAgreedConsent(
                            userId,
                            List.of(version.getId())
                    );

            // then
            assertThat(result)
                    .isFalse();
        }
    }
}