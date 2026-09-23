package com.todak_todag.discharge_service.discharge.infrastructure.persistence.query;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.discharge_service.discharge.domain.entity.Discharge;
import com.todak_todag.discharge_service.discharge.domain.entity.DischargeStatus;
import com.todak_todag.discharge_service.discharge.domain.entity.QDischarge;
import com.todak_todag.discharge_service.discharge.domain.repository.query.DischargeQueryRepository;
import com.todak_todag.discharge_service.discharge.infrastructure.persistence.JpaDischargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DischargeQueryRepositoryImpl implements DischargeQueryRepository {

    private final JpaDischargeRepository jpaDischargeRepository;
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Optional<Discharge> findById(UUID dischargeId) {
        return jpaDischargeRepository.findById(dischargeId);
    }

    @Override
    public Page<Discharge> search(
            UUID hospitalStaffId,
            DischargeStatus status,
            LocalDate scheduledDate,
            Pageable pageable
    ) {
        QDischarge discharge = QDischarge.discharge;

        List<Discharge> content = jpaQueryFactory
                .selectFrom(discharge)
                .where(
                        discharge.hospitalStaffId.eq(hospitalStaffId),
                        statusEq(discharge, status),
                        scheduledDateEq(discharge, scheduledDate)
                )
                .orderBy(resolveOrder(discharge, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(discharge.count())
                .from(discharge)
                .where(
                        discharge.hospitalStaffId.eq(hospitalStaffId),
                        statusEq(discharge, status),
                        scheduledDateEq(discharge, scheduledDate)
                );

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                countQuery::fetchOne
        );
    }

    private BooleanExpression statusEq(
            QDischarge discharge,
            DischargeStatus status
    ) {
        return status != null
                ? discharge.status.eq(status)
                : null;
    }

    private BooleanExpression scheduledDateEq(
            QDischarge discharge,
            LocalDate scheduledDate
    ) {
        return scheduledDate != null
                ? discharge.scheduledDate.eq(scheduledDate)
                : null;
    }

    private OrderSpecifier<?> resolveOrder(
            QDischarge discharge,
            Sort sort
    ) {
        Sort.Order order = sort.getOrderFor("createdAt");

        if (order == null || order.isDescending()) {
            return discharge.createdAt.desc();
        }

        return discharge.createdAt.asc();
    }
}