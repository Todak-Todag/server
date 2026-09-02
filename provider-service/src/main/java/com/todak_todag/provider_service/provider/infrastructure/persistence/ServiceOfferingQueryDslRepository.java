package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.provider_service.provider.application.result.ServiceOfferingSearchResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.todak_todag.provider_service.provider.domain.entity.QProvideService.provideService;
import static com.todak_todag.provider_service.provider.domain.entity.QServiceOffering.serviceOffering;

@Repository
@RequiredArgsConstructor
public class ServiceOfferingQueryDslRepository {

    private final JPAQueryFactory queryFactory;

    public Page<ServiceOfferingSearchResult> searchByProviderId(UUID providerId, Pageable pageable) {
        List<ServiceOfferingSearchResult> content = queryFactory
                .select(Projections.constructor(
                        ServiceOfferingSearchResult.class,
                        serviceOffering.id,
                        serviceOffering.provideServiceId,
                        provideService.name,
                        serviceOffering.createdAt
                ))
                .from(serviceOffering)
                .join(provideService).on(provideService.id.eq(serviceOffering.provideServiceId))
                .where(serviceOffering.providerId.eq(providerId))
                .orderBy(toOrderSpecifier(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(serviceOffering.count())
                .from(serviceOffering)
                .join(provideService).on(provideService.id.eq(serviceOffering.provideServiceId))
                .where(serviceOffering.providerId.eq(providerId))
                .fetchOne();

        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private OrderSpecifier<Instant> toOrderSpecifier(Sort sort) {
        Sort.Order order = sort.getOrderFor("createdAt");
        boolean ascending = order != null && order.isAscending();

        return new OrderSpecifier<>(
                ascending ? Order.ASC : Order.DESC,
                serviceOffering.createdAt
        );
    }
}