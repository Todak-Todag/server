package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.schedule_service.schedule.domain.entity.CarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.QCarePlanServiceResult;
import com.todak_todag.schedule_service.schedule.domain.entity.QServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.repository.query.CarePlanServiceResultQueryRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataCarePlanServiceResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CarePlanServiceResultQueryRepositoryImpl implements CarePlanServiceResultQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final SpringDataCarePlanServiceResultRepository springDataCarePlanServiceResultRepository;

    @Override
    public Optional<CarePlanServiceResult> findById(UUID serviceResultId) {
        return springDataCarePlanServiceResultRepository.findByServiceResultIdAndDeletedAtIsNull(serviceResultId);
    }

    @Override
    public Page<CarePlanServiceResult> search(
            List<UUID> servicePreferenceIds,
            List<UUID> serviceOfferingIds,
            Pageable pageable
    ) {
        QCarePlanServiceResult result = QCarePlanServiceResult.carePlanServiceResult;
        QServiceSchedule schedule = QServiceSchedule.serviceSchedule;

        List<CarePlanServiceResult> content = jpaQueryFactory
                .selectFrom(result)
                // 연관관계 매핑이 아니라 논리 FK이므로 on 절로 직접 조인
                .join(schedule).on(result.serviceScheduleId.eq(schedule.id))
                .where(
                        servicePreferenceIdsIn(schedule, servicePreferenceIds),
                        serviceOfferingIdsIn(schedule, serviceOfferingIds),
                        schedule.deletedAt.isNull(),
                        result.deletedAt.isNull()
                )
                .orderBy(resolveOrder(result, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(result.count())
                .from(result)
                .join(schedule).on(result.serviceScheduleId.eq(schedule.id))
                .where(
                        servicePreferenceIdsIn(schedule, servicePreferenceIds),
                        serviceOfferingIdsIn(schedule, serviceOfferingIds),
                        schedule.deletedAt.isNull(),
                        result.deletedAt.isNull()
                );

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                countQuery::fetchOne
        );
    }

    // PageableFactory가 정렬 필드를 항상 createdAt으로 고정하므로(방향만 파싱) createdAt 기준 정렬만 지원
    private OrderSpecifier<?> resolveOrder(
            QCarePlanServiceResult result,
            Sort sort
    ) {
        Sort.Order order = sort.getOrderFor("createdAt");

        if (order == null || order.isDescending()) {
            return result.createdAt.desc();
        }

        return result.createdAt.asc();
    }

    // 퇴원 예정자 소유권 필터 — care-plan-service Internal API가 반환한 servicePreferenceId 목록
    private BooleanExpression servicePreferenceIdsIn(
            QServiceSchedule schedule,
            List<UUID> servicePreferenceIds
    ) {
        return servicePreferenceIds != null
                ? schedule.servicePreferenceId.in(servicePreferenceIds)
                : null;
    }

    // 서비스 제공자 소유권 필터 — provider-service Internal API가 반환한 serviceOfferingId 목록
    private BooleanExpression serviceOfferingIdsIn(
            QServiceSchedule schedule,
            List<UUID> serviceOfferingIds
    ) {
        return serviceOfferingIds != null
                ? schedule.serviceOfferingId.in(serviceOfferingIds)
                : null;
    }
}
