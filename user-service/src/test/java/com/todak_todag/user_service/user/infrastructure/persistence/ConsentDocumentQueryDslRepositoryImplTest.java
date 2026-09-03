package com.todak_todag.user_service.user.infrastructure.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
+ import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ConsentDocumentQueryDslRepositoryImplTest {

    @Autowired
    private EntityManager entityManager;

    private ConsentDocumentQueryDslRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new ConsentDocumentQueryDslRepositoryImpl(
                new JPAQueryFactory(entityManager)
        );
    }

    @Test
    @DisplayName("현재 시점 이전 버전 중 effectiveAt이 가장 최근인 버전을 조회한다")
    void findAllCurrent_success() {
        // given
        LocalDateTime now =
                LocalDateTime.of(2026, 9, 4, 12, 0);

        UUID documentId = UUID.randomUUID();

        ConsentDocument document = createDocument(
                documentId,
                "TERMS_OF_SERVICE",
                "서비스 이용약관",
                true
        );

        entityManager.persist(document);

        ConsentDocumentVersion v1 = createVersion(
                UUID.randomUUID(),
                documentId,
                "v1",
                "v1 약관 내용",
                LocalDateTime.of(2026, 7, 1, 0, 0)
        );

        ConsentDocumentVersion v2 = createVersion(
                UUID.randomUUID(),
                documentId,
                "v2",
                "v2 약관 내용",
                LocalDateTime.of(2026, 8, 1, 0, 0)
        );

        ConsentDocumentVersion v3 = createVersion(
                UUID.randomUUID(),
                documentId,
                "v3",
                "v3 약관 내용",
                LocalDateTime.of(2026, 10, 1, 0, 0)
        );

        entityManager.persist(v1);
        entityManager.persist(v2);
        entityManager.persist(v3);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ConsentDocumentCurrentView> results =
                repository.findAllCurrent(now);

        // then
        assertThat(results).hasSize(1);

        ConsentDocumentCurrentView result = results.getFirst();

        assertThat(result.consentDocumentId())
                .isEqualTo(documentId);

        assertThat(result.consentDocumentVersionId())
                .isEqualTo(v2.getId());

        assertThat(result.version())
                .isEqualTo("v2");

        assertThat(result.isRequired())
                .isTrue();
    }

    @Test
    @DisplayName("아직 적용 시점이 도래하지 않은 약관은 조회하지 않는다")
    void findAllCurrent_futureVersion() {
        // given
        LocalDateTime now =
                LocalDateTime.of(2026, 9, 4, 12, 0);

        UUID documentId = UUID.randomUUID();

        ConsentDocument document = createDocument(
                documentId,
                "PRIVACY_POLICY",
                "개인정보 처리방침",
                true
        );

        ConsentDocumentVersion futureVersion = createVersion(
                UUID.randomUUID(),
                documentId,
                "v1",
                "약관 내용",
                LocalDateTime.of(2026, 10, 1, 0, 0)
        );

        entityManager.persist(document);
        entityManager.persist(futureVersion);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ConsentDocumentCurrentView> results =
                repository.findAllCurrent(now);

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("전달된 버전이 최신 버전이 아니면 조회하지 않는다")
    void findAllCurrentByVersionIds_notCurrentVersion() {
        // given
        LocalDateTime now =
                LocalDateTime.of(2026, 9, 4, 12, 0);

        UUID documentId = UUID.randomUUID();
        UUID oldVersionId = UUID.randomUUID();

        ConsentDocument document = createDocument(
                documentId,
                "TERMS_OF_SERVICE",
                "서비스 이용약관",
                true
        );

        ConsentDocumentVersion v1 = createVersion(
                oldVersionId,
                documentId,
                "v1",
                "v1 약관 내용",
                LocalDateTime.of(2026, 7, 1, 0, 0)
        );

        ConsentDocumentVersion v2 = createVersion(
                UUID.randomUUID(),
                documentId,
                "v2",
                "v2 약관 내용",
                LocalDateTime.of(2026, 8, 1, 0, 0)
        );

        entityManager.persist(document);
        entityManager.persist(v1);
        entityManager.persist(v2);

        entityManager.flush();
        entityManager.clear();

        // when
        List<ConsentDocumentCurrentView> results =
                repository.findAllCurrentByVersionIds(
                        List.of(oldVersionId),
                        now
                );

        // then
        assertThat(results).isEmpty();
    }

    private ConsentDocument createDocument(
            UUID id,
            String consentType,
            String title,
            boolean required
    ) {
        ConsentDocument document =
                ReflectionTestUtils.invokeConstructor(ConsentDocument.class);

        ReflectionTestUtils.setField(document, "id", id);
        ReflectionTestUtils.setField(document, "consentType", consentType);
        ReflectionTestUtils.setField(document, "title", title);
        ReflectionTestUtils.setField(document, "required", required);

        return document;
    }

    private ConsentDocumentVersion createVersion(
            UUID id,
            UUID consentDocumentId,
            String version,
            String content,
            LocalDateTime effectiveAt
    ) {
        ConsentDocumentVersion documentVersion =
                ReflectionTestUtils.invokeConstructor(
                        ConsentDocumentVersion.class
                );

        ReflectionTestUtils.setField(documentVersion, "id", id);
        ReflectionTestUtils.setField(
                documentVersion,
                "consentDocumentId",
                consentDocumentId
        );
        ReflectionTestUtils.setField(documentVersion, "version", version);
        ReflectionTestUtils.setField(documentVersion, "content", content);
        ReflectionTestUtils.setField(
                documentVersion,
                "effectiveAt",
                effectiveAt
        );

        return documentVersion;
    }
}