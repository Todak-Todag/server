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

import java.time.LocalDate;
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
        // Care Plan 목록 조회
        List<CarePlan> carePlans = queryFactory
                .selectFrom(carePlan)
                .where(
                        carePlan.patientId.eq(carePlanSearchQuery.patientId()),
                        statusEq(carePlanSearchQuery.status()),
                        startDateGoe(carePlanSearchQuery.startDate()),
                        finishDateLoe(carePlanSearchQuery.finishDate()),
                        carePlan.deletedAt.isNull()
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 조회 건수
        Long total = queryFactory
                .select(carePlan.count())
                .from(carePlan)
                .where(
                        carePlan.patientId.eq(carePlanSearchQuery.patientId()),
                        statusEq(carePlanSearchQuery.status()),
                        startDateGoe(carePlanSearchQuery.startDate()),
                        finishDateLoe(carePlanSearchQuery.finishDate()),
                        carePlan.deletedAt.isNull()
                )
                .fetchOne();

        return new PageImpl<>(
                carePlans,
                pageable,
                total != null ? total : 0L
        );
    }

    private BooleanExpression statusEq(CarePlanStatus status) {
        if (status == null) {
            return null;
        }

        return carePlan.status.eq(status);
    }

    private BooleanExpression startDateGoe(
            LocalDate startDate
    ) {
        if (startDate == null) {
            return null;
        }

        return carePlan.startDate.goe(startDate);
    }

    private BooleanExpression finishDateLoe(
            LocalDate finishDate
    ) {
        if (finishDate == null) {
            return null;
        }

        return carePlan.finishDate.loe(finishDate);
    }
}
