package com.todak_todag.user_service.user.infrastructure.persistence.query;

import com.todak_todag.user_service.support.PostgresTestSupport;
import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    @DisplayName("현재 시점에 적용 가능한 가장 최신 약관 버전을 조회한다")
    void findAllCurrent() {

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
    @DisplayName("미래에 적용될 약관 버전은 현재 약관으로 조회하지 않는다")
    void futureVersionIsNotCurrent() {

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

        assertThat(result.getFirst().version())
                .isEqualTo("1.0");
    }
}