package com.todak_todag.user_service.user.infrastructure.persistence.query;

import com.querydsl.core.types.Projections;
import com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus;
import com.todak_todag.user_service.user.domain.repository.query.ConsentHistoryView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentQueryRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import com.querydsl.jpa.impl.JPAQueryFactory;

import java.util.List;
import java.util.UUID;

import static com.todak_todag.user_service.user.domain.entity.QConsent.consent;
import static com.todak_todag.user_service.user.domain.entity.QConsentDocument.consentDocument;
import static com.todak_todag.user_service.user.domain.entity.QConsentDocumentVersion.consentDocumentVersion;

@Repository
@RequiredArgsConstructor
public class ConsentQueryRepositoryImpl
        implements ConsentQueryRepository {

    private final JpaConsentRepository jpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public boolean existsAgreedConsent(
            UUID userId,
            List<UUID> consentDocumentVersionIds
    ) {
        return jpaRepository
                .existsByUserIdAndConsentDocumentVersionIdInAndStatus(
                        userId,
                        consentDocumentVersionIds,
                        ConsentStatus.AGREED
                );
    }

    @Override
    public List<ConsentHistoryView> findAllByUserId(
            UUID userId
    ) {
        return queryFactory
                .select(
                        Projections.constructor(
                                ConsentHistoryView.class,
                                consent.id,
                                consentDocument.id,
                                consentDocumentVersion.id,
                                consentDocument.title,
                                consentDocumentVersion.version,
                                consent.status,
                                consent.agreedAt,
                                consent.withdrawnAt
                        )
                )
                .from(consent)
                .join(consentDocumentVersion)
                .on(
                        consentDocumentVersion.id.eq(
                                consent.consentDocumentVersionId
                        )
                )
                .join(consentDocument)
                .on(
                        consentDocument.id.eq(
                                consentDocumentVersion.consentDocumentId
                        )
                )
                .where(
                        consent.userId.eq(userId)
                )
                .orderBy(consent.agreedAt.desc())
                .fetch();
    }
}