package com.todak_todag.schedule_service.schedule.infrastructure.persistence.query;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.schedule_service.schedule.domain.entity.MatchingAttemptStatus;
import com.todak_todag.schedule_service.schedule.domain.entity.QServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.entity.QServiceSchedule;
import com.todak_todag.schedule_service.schedule.domain.entity.ServiceMatchingAttempt;
import com.todak_todag.schedule_service.schedule.domain.repository.query.ServiceMatchingAttemptQueryRepository;
import com.todak_todag.schedule_service.schedule.infrastructure.persistence.SpringDataServiceMatchingAttemptRepository;
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
public class ServiceMatchingAttemptQueryRepositoryImpl implements ServiceMatchingAttemptQueryRepository {

    private final JPAQueryFactory jpaQueryFactory;
    private final SpringDataServiceMatchingAttemptRepository springDataServiceMatchingAttemptRepository;

    @Override
    public Optional<ServiceMatchingAttempt> findById(UUID matchingAttemptId) {
        return springDataServiceMatchingAttemptRepository.findByIdAndDeletedAtIsNull(matchingAttemptId);
    }

    // 매칭 실패 내역 목록 조회
    // 소유권은 Internal API로 받아온 ID 목록의 IN 조건으로 걸고, 페이지네이션은 DB 레벨에서 처리
    @Override
    public Page<ServiceMatchingAttempt> search(
            List<UUID> servicePreferenceIds,
            MatchingAttemptStatus status,
            boolean excludeAlreadyScheduled,
            Pageable pageable
    ) {
        // 방어 코드 - 소유권 필터 없이 전체 조회되는 것을 방지
        if (servicePreferenceIds == null || servicePreferenceIds.isEmpty()) {
            throw new IllegalArgumentException("[Schedule] 매칭 시도 내역 조회에는 servicePreferenceId 목록이 필요합니다.");
        }

        QServiceMatchingAttempt attempt = QServiceMatchingAttempt.serviceMatchingAttempt;

        List<ServiceMatchingAttempt> content = jpaQueryFactory
                .selectFrom(attempt)
                .where(
                        attempt.servicePreferenceId.in(servicePreferenceIds),
                        statusEq(attempt, status),
                        scheduleNotCreated(attempt, excludeAlreadyScheduled),
                        attempt.deletedAt.isNull()
                )
                .orderBy(resolveOrder(attempt, pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = jpaQueryFactory
                .select(attempt.count())
                .from(attempt)
                .where(
                        attempt.servicePreferenceId.in(servicePreferenceIds),
                        statusEq(attempt, status),
                        scheduleNotCreated(attempt, excludeAlreadyScheduled),
                        attempt.deletedAt.isNull()
                );

        return PageableExecutionUtils.getPage(
                content,
                pageable,
                countQuery::fetchOne
        );
    }

    // "아직 재매칭이 필요한 실패 내역"만 남기는 조건
    //
    // 매칭 시도는 이벤트 수신마다 누적(append)되므로 1차 실패 후 2차에 성공해도 1차 FAILED 레코드는 영구히 존재
    // 성공하면 p_service_schedules에 일정이 생성
    // 따라서 "해당 희망 일정에 일정 레코드가 아예 없는가"로 판정하면 이미 해소된 과거 실패를 자연히 걸러낼 수 있음
    // 재매칭 실패는 기존 일정이 SCHEDULED로 복구되어 레코드가 남으므로 이 조건에서 함께 제외
    private BooleanExpression scheduleNotCreated(
            QServiceMatchingAttempt attempt,
            boolean excludeAlreadyScheduled
    ) {
        if (!excludeAlreadyScheduled) {
            return null;
        }

        QServiceSchedule schedule = QServiceSchedule.serviceSchedule;

        return JPAExpressions
                .selectOne()
                .from(schedule)
                .where(
                        schedule.servicePreferenceId.eq(attempt.servicePreferenceId),
                        schedule.deletedAt.isNull()
                )
                .notExists();
    }

    // PageableFactory가 정렬 필드를 항상 createdAt으로 고정하므로(방향만 파싱) createdAt 기준 정렬만 지원
    private OrderSpecifier<?> resolveOrder(
            QServiceMatchingAttempt attempt,
            Sort sort
    ) {
        Sort.Order order = sort.getOrderFor("createdAt");

        if (order == null || order.isDescending()) {
            return attempt.createdAt.desc();
        }

        return attempt.createdAt.asc();
    }

    private BooleanExpression statusEq(
            QServiceMatchingAttempt attempt,
            MatchingAttemptStatus status
    ) {
        return status != null
                ? attempt.status.eq(status)
                : null;
    }
}
