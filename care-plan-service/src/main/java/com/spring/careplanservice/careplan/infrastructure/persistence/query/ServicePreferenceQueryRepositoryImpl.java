package com.spring.careplanservice.careplan.infrastructure.persistence.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spring.careplanservice.careplan.domain.entity.CarePlanServicePreference;
import com.spring.careplanservice.careplan.domain.repository.query.ServicePreferenceQueryRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.JpaServicePreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public List<CarePlanServicePreference> findAllByPlanServiceId(
            UUID planServiceId
    ) {
        return jpaCarePlanPreferenceRepository.findAllByPlanServiceIdAndDeletedAtIsNull(
                planServiceId
        );
    }
}
