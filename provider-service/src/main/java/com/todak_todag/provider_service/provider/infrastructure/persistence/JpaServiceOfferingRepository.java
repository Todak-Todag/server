package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    boolean existsByProviderIdAndProvideServiceId(UUID providerId, UUID provideServiceId);

    List<ServiceOffering> findAllByRegionIdAndProvideServiceId(UUID regionId, UUID provideServiceId);

    // 삭제 이력까지 조회하는 내부 전용 쿼리
    // @SQLRestriction은 JPQL에도 적용되므로 네이티브 쿼리로 우회한다
    // 네이티브 쿼리에는 hibernate default_schema가 붙지 않아 스키마를 직접 적는다
    @Query(value = """
            select provider_id
            from provider_schema.p_provide_service_offerings
            where service_offering_id = :serviceOfferingId
            """, nativeQuery = true)
    Optional<UUID> findProviderIdIncludingDeleted(@Param("serviceOfferingId") UUID serviceOfferingId);

    @Query(value = """
            select service_offering_id
            from provider_schema.p_provide_service_offerings
            where provider_id = :providerId
            """, nativeQuery = true)
    List<UUID> findIdsByProviderIdIncludingDeleted(@Param("providerId") UUID providerId);
}