package com.todak_todag.user_service.user.infrastructure.persistence;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.user_service.user.domain.entity.QConsentDocumentVersion;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.todak_todag.user_service.user.domain.entity.QConsentDocument.consentDocument;
import static com.todak_todag.user_service.user.domain.entity.QConsentDocumentVersion.consentDocumentVersion;

@RequiredArgsConstructor
public class ConsentDocumentQueryDslRepositoryImpl
        implements ConsentDocumentQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ConsentDocumentCurrentView> findAllCurrent(
            LocalDateTime now
    ) {
        QConsentDocumentVersion subVersion =
                new QConsentDocumentVersion("subVersion");

        return queryFactory
                .select(Projections.constructor(
                        ConsentDocumentCurrentView.class,
                        consentDocument.id,
                        consentDocumentVersion.id,
                        consentDocument.consentType,
                        consentDocument.title,
                        consentDocumentVersion.version,
                        consentDocument.required
                ))
                .from(consentDocument)
                .join(consentDocumentVersion)
                .on(
                        consentDocumentVersion.consentDocumentId
                                .eq(consentDocument.id)
                )
                .where(
                        // 사용 종료된 약관 제외
                        consentDocument.deletedAt.isNull(),

                        // 현재 시점에 적용 가능한 버전만 조회
                        consentDocumentVersion.effectiveAt.loe(now),

                        // 약관별 가장 최근 적용 버전 선택
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
                .select(Projections.constructor(
                        ConsentDocumentCurrentView.class,
                        consentDocument.id,
                        consentDocumentVersion.id,
                        consentDocument.consentType,
                        consentDocument.title,
                        consentDocumentVersion.version,
                        consentDocument.required
                ))
                .from(consentDocument)
                .join(consentDocumentVersion)
                .on(
                        consentDocumentVersion.consentDocumentId
                                .eq(consentDocument.id)
                )
                .where(
                        consentDocument.deletedAt.isNull(),

                        consentDocumentVersion.id
                                .in(consentDocumentVersionIds),

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
}