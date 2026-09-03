package com.todak_todag.user_service.user.infrastructure.persistence;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.user_service.user.application.query.RegionFindAdminQuery;
import com.todak_todag.user_service.user.domain.entity.Region;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static com.todak_todag.user_service.user.domain.entity.QRegion.region;

@RequiredArgsConstructor
public class RegionQueryDslRepositoryImpl implements RegionQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Region> findAllByAdminConditions(
            RegionFindAdminQuery query,
            Pageable pageable
    ) {
        BooleanBuilder conditions = createConditions(query);

        // 조건에 맞는 지역 목록 조회
        List<Region> content = queryFactory
                .selectFrom(region)
                .where(conditions)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // 전체 조회 개수 계산
        JPAQuery<Long> countQuery = queryFactory
                .select(region.count())
                .from(region)
                .where(conditions);

        Long total = countQuery.fetchOne();

        return new PageImpl<>(
                content,
                pageable,
                total != null ? total : 0L
        );
    }

    private BooleanBuilder createConditions(RegionFindAdminQuery query) {
        BooleanBuilder builder = new BooleanBuilder();

        // 논리 삭제된 지역 제외
        builder.and(region.deletedAt.isNull());

        if (query.province() != null && !query.province().isBlank()) {
            builder.and(region.province.eq(query.province()));
        }

        if (query.district() != null && !query.district().isBlank()) {
            builder.and(region.district.eq(query.district()));
        }

        if (query.regionCode() != null && !query.regionCode().isBlank()) {
            builder.and(region.regionCode.eq(query.regionCode()));
        }

        if (query.isActive() != null) {
            builder.and(region.active.eq(query.isActive()));
        }

        return builder;
    }
}