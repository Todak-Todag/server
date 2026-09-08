package com.todak_todag.user_service.user.infrastructure.persistence.query;

import com.todak_todag.user_service.support.PostgresTestSupport;
import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentDetailView;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentVersionRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConsentDocumentQueryRepositoryIntegrationTest
        extends PostgresTestSupport {

    @Autowired
    private ConsentDocumentQueryRepositoryImpl repository;

    @Autowired
    private JpaConsentDocumentRepository documentRepository;

    @Autowired
    private JpaConsentDocumentVersionRepository versionRepository;

    @BeforeEach
    void setUp() {
        versionRepository.deleteAll();
        documentRepository.deleteAll();
    }

    @Nested
    @DisplayName("현재 적용 약관 조회")
    class FindCurrent {

        @Test
        @DisplayName("현재 시점에 적용 가능한 가장 최신 약관 버전을 조회한다")
        void findLatestCurrentVersion() {

            // given
            LocalDateTime now =
                    LocalDateTime.of(2026, 9, 8, 12, 0);

            ConsentDocument document =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.PERSONAL_INFORMATION,
                            "개인정보 수집 및 이용 동의",
                            true
                    );

            documentRepository.saveAndFlush(document);

            ConsentDocumentVersion version1 =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "1.0",
                            "개인정보 약관 1.0",
                            now.minusDays(30)
                    );

            ConsentDocumentVersion version11 =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "1.1",
                            "개인정보 약관 1.1",
                            now.minusDays(5)
                    );

            ConsentDocumentVersion futureVersion =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "2.0",
                            "개인정보 약관 2.0",
                            now.plusDays(10)
                    );

            versionRepository.saveAll(
                    List.of(
                            version1,
                            version11,
                            futureVersion
                    )
            );

            versionRepository.flush();

            // when
            List<ConsentDocumentCurrentView> result =
                    repository.findAllCurrent(now);

            // then
            assertThat(result)
                    .hasSize(1);

            ConsentDocumentCurrentView current =
                    result.getFirst();

            assertThat(current.consentDocumentId())
                    .isEqualTo(document.getId());

            assertThat(current.consentDocumentVersionId())
                    .isEqualTo(version11.getId());

            assertThat(current.version())
                    .isEqualTo("1.1");

            assertThat(current.title())
                    .isEqualTo("개인정보 수집 및 이용 동의");

            assertThat(current.isRequired())
                    .isTrue();
        }

        @Test
        @DisplayName("미래에 적용될 버전은 현재 약관으로 조회하지 않는다")
        void excludeFutureVersion() {

            // given
            LocalDateTime now =
                    LocalDateTime.of(2026, 9, 8, 12, 0);

            ConsentDocument document =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.MARKETING_INFORMATION,
                            "마케팅 정보 수신 동의",
                            false
                    );

            documentRepository.saveAndFlush(document);

            ConsentDocumentVersion currentVersion =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "1.0",
                            "현재 마케팅 약관",
                            now.minusDays(1)
                    );

            ConsentDocumentVersion futureVersion =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "2.0",
                            "미래 마케팅 약관",
                            now.plusDays(7)
                    );

            versionRepository.saveAll(
                    List.of(
                            currentVersion,
                            futureVersion
                    )
            );

            versionRepository.flush();

            // when
            List<ConsentDocumentCurrentView> result =
                    repository.findAllCurrent(now);

            // then
            assertThat(result)
                    .hasSize(1);

            assertThat(result.getFirst().consentDocumentVersionId())
                    .isEqualTo(currentVersion.getId());

            assertThat(result.getFirst().version())
                    .isEqualTo("1.0");
        }

        @Test
        @DisplayName("여러 약관에서 각각 현재 적용 중인 최신 버전을 조회한다")
        void findLatestVersionForEachDocument() {

            // given
            LocalDateTime now =
                    LocalDateTime.of(2026, 9, 8, 12, 0);

            ConsentDocument personal =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.PERSONAL_INFORMATION,
                            "개인정보 수집 및 이용 동의",
                            true
                    );

            ConsentDocument marketing =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.MARKETING_INFORMATION,
                            "마케팅 정보 수신 동의",
                            false
                    );

            documentRepository.saveAll(
                    List.of(
                            personal,
                            marketing
                    )
            );

            documentRepository.flush();

            ConsentDocumentVersion personalV1 =
                    ConsentDocumentVersion.create(
                            personal.getId(),
                            "1.0",
                            "개인정보 약관 1.0",
                            now.minusDays(30)
                    );

            ConsentDocumentVersion personalV2 =
                    ConsentDocumentVersion.create(
                            personal.getId(),
                            "2.0",
                            "개인정보 약관 2.0",
                            now.minusDays(1)
                    );

            ConsentDocumentVersion marketingV1 =
                    ConsentDocumentVersion.create(
                            marketing.getId(),
                            "1.0",
                            "마케팅 약관 1.0",
                            now.minusDays(10)
                    );

            ConsentDocumentVersion marketingFuture =
                    ConsentDocumentVersion.create(
                            marketing.getId(),
                            "2.0",
                            "마케팅 약관 2.0",
                            now.plusDays(10)
                    );

            versionRepository.saveAll(
                    List.of(
                            personalV1,
                            personalV2,
                            marketingV1,
                            marketingFuture
                    )
            );

            versionRepository.flush();

            // when
            List<ConsentDocumentCurrentView> result =
                    repository.findAllCurrent(now);

            // then
            assertThat(result)
                    .hasSize(2);

            assertThat(result)
                    .extracting(
                            ConsentDocumentCurrentView::title,
                            ConsentDocumentCurrentView::version
                    )
                    .containsExactlyInAnyOrder(
                            tuple(
                                    "개인정보 수집 및 이용 동의",
                                    "2.0"
                            ),
                            tuple(
                                    "마케팅 정보 수신 동의",
                                    "1.0"
                            )
                    );
        }

        @Test
        @DisplayName("전달된 버전 ID 중 현재 적용 중인 버전만 조회한다")
        void findCurrentByVersionIds() {

            // given
            LocalDateTime now =
                    LocalDateTime.of(2026, 9, 8, 12, 0);

            ConsentDocument document =
                    ConsentDocument.create(
                            ConsentDocument.ConsentType.SENSITIVE_INFORMATION,
                            "민감정보 수집 및 이용 동의",
                            true
                    );

            documentRepository.saveAndFlush(document);

            ConsentDocumentVersion oldVersion =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "1.0",
                            "민감정보 약관 1.0",
                            now.minusDays(30)
                    );

            ConsentDocumentVersion currentVersion =
                    ConsentDocumentVersion.create(
                            document.getId(),
                            "2.0",
                            "민감정보 약관 2.0",
                            now.minusDays(1)
                    );

            versionRepository.saveAll(
                    List.of(
                            oldVersion,
                            currentVersion
                    )
            );

            versionRepository.flush();

            // when
            List<ConsentDocumentCurrentView> result =
                    repository.findAllCurrentByVersionIds(
                            List.of(
                                    oldVersion.getId(),
                                    currentVersion.getId()
                            ),
                            now
                    );

            // then
            assertThat(result)
                    .hasSize(1);

            assertThat(result.getFirst().consentDocumentVersionId())
                    .isEqualTo(currentVersion.getId());

            assertThat(result.getFirst().version())
                    .isEqualTo("2.0");
        }
    }

    @Nested
    @DisplayName("약관 버전 상세 조회")
    class FindDetail {

        @Test
        @DisplayName("버전 ID로 약관 문서와 버전 상세 정보를 조회한다")
        void findDetailByVersionId() {

            // given
            LocalDateTime effectiveAt =
                    LocalDateTime.of(2026, 9, 1, 0, 0);

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
                            "개인정보 약관 본문",
                            effectiveAt
                    );

            versionRepository.saveAndFlush(version);

            // when
            Optional<ConsentDocumentDetailView> result =
                    repository.findDetailByVersionId(
                            version.getId()
                    );

            // then
            assertThat(result)
                    .isPresent();

            ConsentDocumentDetailView detail =
                    result.orElseThrow();

            assertThat(detail.consentDocumentId())
                    .isEqualTo(document.getId());

            assertThat(detail.consentDocumentVersionId())
                    .isEqualTo(version.getId());

            assertThat(detail.title())
                    .isEqualTo("개인정보 수집 및 이용 동의");

            assertThat(detail.version())
                    .isEqualTo("1.0");

            assertThat(detail.content())
                    .isEqualTo("개인정보 약관 본문");

            assertThat(detail.isRequired())
                    .isTrue();

            assertThat(detail.effectiveAt())
                    .isEqualTo(effectiveAt);
        }
    }

    @Nested
    @DisplayName("약관 버전 중복 확인")
    class ExistsVersion {

        @Test
        @DisplayName("같은 약관에 동일한 버전이 존재하면 true를 반환한다")
        void existsVersion() {

            // given
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
                            LocalDateTime.of(2026, 9, 1, 0, 0)
                    );

            versionRepository.saveAndFlush(version);

            // when
            boolean result =
                    repository.existsVersion(
                            document.getId(),
                            "1.0"
                    );

            // then
            assertThat(result)
                    .isTrue();
        }
    }
}