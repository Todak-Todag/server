package com.todak_todag.user_service.user.infrastructure.persistence.query;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.user_service.user.domain.entity.QConsentDocumentVersion;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentDetailView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.todak_todag.user_service.user.domain.entity.QConsentDocument.consentDocument;
import static com.todak_todag.user_service.user.domain.entity.QConsentDocumentVersion.consentDocumentVersion;

@Repository
@RequiredArgsConstructor
public class ConsentDocumentQueryRepositoryImpl
        implements ConsentDocumentQueryRepository {

    private final JpaConsentDocumentRepository jpaRepo;
    private final JPAQueryFactory queryFactory;

    @Override
    public List<ConsentDocumentCurrentView> findAllCurrent(
            LocalDateTime now
    ) {
        QConsentDocumentVersion subVersion =
                new QConsentDocumentVersion("subVersion");

        return queryFactory
                .select(
                        Projections.constructor(
                                ConsentDocumentCurrentView.class,
                                consentDocument.id,
                                consentDocumentVersion.id,
                                consentDocument.consentType,
                                consentDocument.title,
                                consentDocumentVersion.version,
                                consentDocument.required
                        )
                )
                .from(consentDocument)
                .join(consentDocumentVersion)
                .on(
                        consentDocumentVersion.consentDocumentId
                                .eq(consentDocument.id)
                )
                .where(
                        consentDocument.deletedAt.isNull(),
                        consentDocumentVersion.effectiveAt.loe(now),

                        // 약관별 현재 적용 가능한 가장 최근 버전 조회
                        consentDocumentVersion.effectiveAt.eq(
                                JPAExpressions
                                        .select(subVersion.effectiveAt.max())
                                        .from(subVersion)
                                        .where(
                                                subVersion.consentDocumentId
                                                        .eq(consentDocument.id),
                                                subVersion.effectiveAt.loe(now)
                                        )
                        )
                )
                .fetch();
    }

    @Override
    public List<ConsentDocumentCurrentView> findAllCurrentByVersionIds(
            List<UUID> consentDocumentVersionIds,
            LocalDateTime now
    ) {
        QConsentDocumentVersion subVersion =
                new QConsentDocumentVersion("subVersion");

        return queryFactory
                .select(
                        Projections.constructor(
                                ConsentDocumentCurrentView.class,
                                consentDocument.id,
                                consentDocumentVersion.id,
                                consentDocument.consentType,
                                consentDocument.title,
                                consentDocumentVersion.version,
                                consentDocument.required
                        )
                )
                .from(consentDocument)
                .join(consentDocumentVersion)
                .on(
                        consentDocumentVersion.consentDocumentId
                                .eq(consentDocument.id)
                )
                .where(
                        consentDocument.deletedAt.isNull(),
                        consentDocumentVersion.id.in(
                                consentDocumentVersionIds
                        ),
                        consentDocumentVersion.effectiveAt.loe(now),

                        // 전달된 버전이 현재 적용 중인 버전인지 확인
                        consentDocumentVersion.effectiveAt.eq(
                                JPAExpressions
                                        .select(subVersion.effectiveAt.max())
                                        .from(subVersion)
                                        .where(
                                                subVersion.consentDocumentId
                                                        .eq(consentDocument.id),
                                                subVersion.effectiveAt.loe(now)
                                        )
                        )
                )
                .fetch();
    }

    @Override
    public Optional<ConsentDocumentDetailView> findDetailByVersionId(
            UUID consentDocumentVersionId
    ) {
        ConsentDocumentDetailView detail = queryFactory
                .select(
                        Projections.constructor(
                                ConsentDocumentDetailView.class,
                                consentDocument.id,
                                consentDocumentVersion.id,
                                consentDocument.consentType,
                                consentDocument.title,
                                consentDocumentVersion.version,
                                consentDocumentVersion.content,
                                consentDocument.required,
                                consentDocumentVersion.effectiveAt
                        )
                )
                .from(consentDocumentVersion)
                .join(consentDocument)
                .on(
                        consentDocument.id.eq(
                                consentDocumentVersion.consentDocumentId
                        )
                )
                .where(
                        consentDocumentVersion.id.eq(
                                consentDocumentVersionId
                        ),
                        // 논리 삭제된 약관은 상세 조회에서 제외
                        consentDocument.deletedAt.isNull()
                )
                .fetchOne();

        return Optional.ofNullable(detail);
    }
}