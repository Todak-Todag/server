package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaServiceOfferingRepository extends JpaRepository<ServiceOffering, UUID> {

    boolean existsByProviderIdAndProvideServiceId(UUID providerId, UUID provideServiceId);

    List<ServiceOffering> findAllByRegionIdAndProvideServiceId(UUID regionId, UUID provideServiceId);

    // 같은 제공 서비스에 대한 쓰기를 한 번에 하나씩 처리하기 위한 행 잠금 조회
    // @SQLRestriction이 함께 적용되어, 잠금을 기다리는 사이 삭제됐다면 빈 값을 반환한다
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ServiceOffering> findWithLockById(UUID id);
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

    // 매칭 점유 계산용 — 제공자들이 가진 모든 제공 서비스(삭제 포함)의 ID와 제공자 ID
    // Postgres는 따옴표 없는 별칭을 소문자로 바꾸므로, 프로젝션 getter와 맞추려고 별칭을 따옴표로 감싼다
    @Query(value = """
            select service_offering_id as "serviceOfferingId", provider_id as "providerId"
            from provider_schema.p_provide_service_offerings
            where provider_id in (:providerIds)
            """, nativeQuery = true)
    List<OfferingOwner> findOwnersByProviderIdInIncludingDeleted(@Param("providerIds") Collection<UUID> providerIds);

    interface OfferingOwner {
        UUID getServiceOfferingId();
        UUID getProviderId();
    }
}