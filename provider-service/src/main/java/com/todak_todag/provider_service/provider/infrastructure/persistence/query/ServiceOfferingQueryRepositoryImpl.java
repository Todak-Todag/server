package com.todak_todag.provider_service.provider.infrastructure.persistence.query;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingView;
import com.todak_todag.provider_service.provider.infrastructure.persistence.JpaServiceOfferingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static com.todak_todag.provider_service.provider.domain.entity.QProvideService.provideService;
import static com.todak_todag.provider_service.provider.domain.entity.QServiceOffering.serviceOffering;

@Repository
@RequiredArgsConstructor
public class ServiceOfferingQueryRepositoryImpl implements ServiceOfferingQueryRepository {

    private final JpaServiceOfferingRepository jpaServiceOfferingRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<ServiceOffering> findById(UUID serviceOfferingId) {
        return jpaServiceOfferingRepository.findById(serviceOfferingId);
    }

    @Override
    public boolean existsByProviderIdAndProvideServiceId(UUID providerId, UUID provideServiceId) {
        return jpaServiceOfferingRepository.existsByProviderIdAndProvideServiceId(providerId, provideServiceId);
    }

    @Override
    public Optional<UUID> findProviderIdIncludingDeleted(UUID serviceOfferingId) {
        return jpaServiceOfferingRepository.findProviderIdIncludingDeleted(serviceOfferingId);
    }

    @Override
    public List<UUID> findIdsByProviderIdIncludingDeleted(UUID providerId) {
        return jpaServiceOfferingRepository.findIdsByProviderIdIncludingDeleted(providerId);
    }

    @Override
    public Page<ServiceOfferingView> searchByProviderId(UUID providerId, Pageable pageable) {
        return search(serviceOffering.providerId.eq(providerId), pageable);
    }

    @Override
    public Page<ServiceOfferingView> searchByRegionId(UUID regionId, Pageable pageable) {
        return search(serviceOffering.regionId.eq(regionId), pageable);
    }

    @Override
    public List<ServiceOffering> findAllByRegionIdAndProvideServiceId(UUID regionId, UUID provideServiceId) {
        return jpaServiceOfferingRepository.findAllByRegionIdAndProvideServiceId(regionId, provideServiceId);
    }

    @Override
    public Map<UUID, UUID> findOfferingProviderIdsIncludingDeleted(Collection<UUID> providerIds) {
        // in () 는 SQL 문법 오류라 빈 목록이면 조회하지 않는다
        if (providerIds.isEmpty()) {
            return Map.of();
        }

        return jpaServiceOfferingRepository.findOwnersByProviderIdInIncludingDeleted(providerIds).stream()
                .collect(Collectors.toMap(
                        JpaServiceOfferingRepository.OfferingOwner::getServiceOfferingId,
                        JpaServiceOfferingRepository.OfferingOwner::getProviderId
                ));
    }

    private Page<ServiceOfferingView> search(BooleanExpression condition, Pageable pageable) {
        List<ServiceOfferingView> content = queryFactory
                .select(Projections.constructor(
                        ServiceOfferingView.class,
                        serviceOffering.id,
                        serviceOffering.providerId,
                        serviceOffering.provideServiceId,
                        provideService.name,
                        serviceOffering.createdAt
                ))
                .from(serviceOffering)
                .join(provideService).on(provideService.id.eq(serviceOffering.provideServiceId))
                .where(condition)
                .orderBy(toOrderSpecifier(pageable.getSort()))
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long total = queryFactory
                .select(serviceOffering.count())
                .from(serviceOffering)
                .join(provideService).on(provideService.id.eq(serviceOffering.provideServiceId))
                .where(condition)
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
