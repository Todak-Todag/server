package com.spring.careplanservice.careplan.infrastructure.persistence.query;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.spring.careplanservice.careplan.application.query.CarePlanSearchQuery;
import com.spring.careplanservice.careplan.domain.entity.CarePlan;
import com.spring.careplanservice.careplan.domain.entity.CarePlanStatus;
import com.spring.careplanservice.careplan.domain.repository.query.CarePlanQueryRepository;
import com.spring.careplanservice.careplan.infrastructure.persistence.repository.JpaCarePlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static com.spring.careplanservice.careplan.domain.entity.QCarePlan.carePlan;


@Repository
@RequiredArgsConstructor
public class CarePlanQueryRepositoryImpl implements CarePlanQueryRepository {
    private final JpaCarePlanRepository jpaCarePlanRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<CarePlan> findById(UUID id) {
        return jpaCarePlanRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<CarePlan> findByPatientIdAndStatuses(
            UUID patientId,
            Set<CarePlanStatus> statuses
    ) {
        return jpaCarePlanRepository
                .findByPatientIdAndStatusInAndDeletedAtIsNull(
                        patientId,
                        statuses
                );
    }

    @Override
    public Page<CarePlan> search(
            CarePlanSearchQuery carePlanSearchQuery,
            Pageable pageable
    ) {
        List<CarePlan> carePlans = queryFactory
                .selectFrom(carePlan)
                .where(
                        carePlan.patientId.eq(carePlanSearchQuery.patientId()),
                        statusEq(carePlanSearchQuery.status()),
                        carePlan.deletedAt.isNull()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
        return new PageImpl<>(
                carePlans,
                pageable,
                carePlans.size()
        );
    }

    private BooleanExpression statusEq(CarePlanStatus status) {
        if (status == null) {
            return null;
        }

        return carePlan.status.eq(status);
    }
}
