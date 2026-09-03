package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.schedule_service.schedule.domain.entity.QServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.entity.ScheduleStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceScheduleQueryRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
public class ServiceScheduleQueryRepositoryImpl implements ServiceScheduleQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final SpringDataServiceScheduleRepository springDataServiceScheduleRepository;

    @Override
    public Optional<ServiceSchedule> findById(UUID serviceScheduleId) {
        return springDataServiceScheduleRepository.findByIdAndDeletedAtIsNull(serviceScheduleId);
    }

    @Override
    public List<ServiceSchedule> findSchedules(
            List<UUID> serviceOfferingIds,
            LocalDate startDate,
            LocalDate endDate,
            List<ScheduleStatus> statuses
    ) {
        QServiceSchedule schedule = QServiceSchedule.serviceSchedule;

        return jpaQueryFactory
                .selectFrom(schedule)
                // serviceOfferingId IN / date BETWEEN / status IN / deletedAt IS NULL 조건으로 검색
                .where(
                        schedule.serviceOfferingId.in(serviceOfferingIds),
                        schedule.date.between(startDate, endDate),
                        schedule.status.in(statuses),
                        schedule.deletedAt.isNull()
                )
                .fetch();
    }

    @Override
    public Page<ServiceSchedule> search(
            List<UUID> servicePreferenceIds,
            List<UUID> serviceOfferingIds,
            ScheduleStatus status,
            LocalDate date,
            Pageable pageable
    ) {
        QServiceSchedule schedule = QServiceSchedule.serviceSchedule;

        List<ServiceSchedule> content = jpaQueryFactory
                .selectFrom(schedule)
                .where(
                        servicePreferenceIdsIn(schedule, servicePreferenceIds),
                        serviceOfferingIdsIn(schedule, serviceOfferingIds),
                        statusEq(schedule, status),
                        dateEq(schedule, date),
                        schedule.deletedAt.isNull()
                )
                .orderBy(resolveOrder(schedule, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(schedule.count())
                .from(schedule)
                .where(
                        servicePreferenceIdsIn(schedule, servicePreferenceIds),
                        serviceOfferingIdsIn(schedule, serviceOfferingIds),
                        statusEq(schedule, status),
                        dateEq(schedule, date),
                        schedule.deletedAt.isNull()
                );

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                countQuery::fetchOne
        );
    }

    // PageableFactory가 정렬 필드를 항상 createdAt으로 고정하므로(방향만 파싱) createdAt 기준 정렬만 지원
    private OrderSpecifier<?> resolveOrder(
            QServiceSchedule schedule,
            Sort sort
    ) {
        Sort.Order order = sort.getOrderFor("createdAt");

        if (order == null || order.isDescending()) {
            return schedule.createdAt.desc();
        }

        return schedule.createdAt.asc();
    }

    private BooleanExpression servicePreferenceIdsIn(
            QServiceSchedule schedule,
            List<UUID> servicePreferenceIds
    ) {
        return servicePreferenceIds != null
                ? schedule.servicePreferenceId.in(servicePreferenceIds)
                : null;
    }

    private BooleanExpression serviceOfferingIdsIn(
            QServiceSchedule schedule,
            List<UUID> serviceOfferingIds
    ) {
        return serviceOfferingIds != null
                ? schedule.serviceOfferingId.in(serviceOfferingIds)
                : null;
    }

    private BooleanExpression statusEq(
            QServiceSchedule schedule,
            ScheduleStatus status
    ) {
        return status != null
                ? schedule.status.eq(status)
                : null;
    }

    private BooleanExpression dateEq(
            QServiceSchedule schedule,
            LocalDate date
    ) {
        return date != null
                ? schedule.date.eq(date)
                : null;
    }
}
