package com.spring.careplanservice.careplan.infrastructure.persistence.query;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spring.careplanservice.careplan.application.result.ServicePreferenceSearchResult;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.JpaServicePreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.spring.careplanservice.careplan.domain.entity.QCarePlan.carePlan;
import static com.spring.careplanservice.careplan.domain.entity.QCarePlanService.carePlanService;
import static com.spring.careplanservice.careplan.domain.entity.QCarePlanServicePreference.carePlanServicePreference;

@Repository
@RequiredArgsConstructor
public class ServicePreferenceQueryRepositoryImpl implements ServicePreferenceQueryRepository {
    private final JpaServicePreferenceRepository jpaCarePlanPreferenceRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<CarePlanServicePreference> findById(UUID id) {
        return jpaCarePlanPreferenceRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public List<UUID> findIdsByPatientId(UUID patientId) {
        return queryFactory
                .select(carePlanServicePreference.id)
                .from(carePlanServicePreference)
                .join(carePlanService)
                .on(carePlanServicePreference.planServiceId.eq(carePlanService.id))
                .join(carePlan)
                .on(carePlanService.carePlanId.eq(carePlan.id))
                .where(
                        carePlan.patientId.eq(patientId),
                        carePlan.deletedAt.isNull(),
                        carePlanService.deletedAt.isNull(),
                        carePlanServicePreference.deletedAt.isNull()
                )
                .fetch();
    }

    @Override
    public List<CarePlanServicePreference> findAllByPlanServiceIds(
            List<UUID> planServiceIds
    ) {
        return jpaCarePlanPreferenceRepository.findAllByPlanServiceIdInAndDeletedAtIsNull(
                planServiceIds
        );
    }

    @Override
    public Page<ServicePreferenceSearchResult> search(
            UUID carePlanId,
            LocalDate preferredDate,
            Pageable pageable
    ) {
        List<ServicePreferenceSearchResult> content = queryFactory
                .select(
                        Projections.constructor(
                                ServicePreferenceSearchResult.class,
                                carePlanServicePreference.id,
                                carePlanService.provideServiceId,
                                carePlanServicePreference.preferredDate,
                                carePlanServicePreference.preferredTimeSlot,
                                carePlanServicePreference.createdAt
                        )
                )
                .from(carePlanServicePreference)
                .join(carePlanService)
                .on(carePlanServicePreference.planServiceId.eq(carePlanService.id))
                .where(
                        carePlanService.carePlanId.eq(carePlanId),
                        preferredDateEq(preferredDate),
                        carePlanService.deletedAt.isNull(),
                        carePlanServicePreference.deletedAt.isNull()
                )
                .orderBy(carePlanServicePreference.preferredDate.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(carePlanServicePreference.count())
                .from(carePlanServicePreference)
                .join(carePlanService)
                .on(carePlanServicePreference.planServiceId.eq(carePlanService.id))
                .where(
                        carePlanService.carePlanId.eq(carePlanId),
                        preferredDateEq(preferredDate),
                        carePlanService.deletedAt.isNull(),
                        carePlanServicePreference.deletedAt.isNull()
                )
                .fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    private BooleanExpression preferredDateEq(
            LocalDate preferredDate
    ) {
        if (preferredDate == null) {
            return null;
        }

        return carePlanServicePreference.preferredDate.eq(preferredDate);
    }
}
