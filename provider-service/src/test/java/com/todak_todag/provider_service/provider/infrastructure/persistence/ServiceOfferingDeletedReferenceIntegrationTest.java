package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.provider.domain.repository.query.ServiceOfferingQueryRepository;
import com.todak_todag.provider_service.support.ContainerTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// deleteAll()은 @SQLRestriction 때문에 논리 삭제된 행을 지우지 못한다
// 테스트마다 무작위 ID를 써서 이전 테스트의 잔여 행과 섞이지 않게 한다
@DisplayName("논리 삭제된 제공 서비스의 과거 참조 조회 통합")
class ServiceOfferingDeletedReferenceIntegrationTest extends ContainerTestSupport {

    @Autowired
    private JpaServiceOfferingRepository jpaServiceOfferingRepository;

    @Autowired
    private ServiceOfferingQueryRepository serviceOfferingQueryRepository;

    private final UUID providerId = UUID.randomUUID();
    private final UUID provideServiceId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();

    private ServiceOffering saveDeleted() {
        ServiceOffering offering = jpaServiceOfferingRepository
                .saveAndFlush(ServiceOffering.of(providerId, provideServiceId, regionId));
        offering.markDeleted(providerId);
        return jpaServiceOfferingRepository.saveAndFlush(offering);
    }

    @Test
    @DisplayName("삭제된 제공 서비스도 담당 제공자를 조회한다")
    void findProviderId_deleted() {
        ServiceOffering deleted = saveDeleted();

        assertThat(serviceOfferingQueryRepository.findProviderIdIncludingDeleted(deleted.getId()))
                .contains(providerId);
    }

    @Test
    @DisplayName("존재하지 않는 제공 서비스는 빈 값을 반환한다")
    void findProviderId_notExists() {
        assertThat(serviceOfferingQueryRepository.findProviderIdIncludingDeleted(UUID.randomUUID()))
                .isEmpty();
    }

    @Test
    @DisplayName("제공자별 ID 목록에 활성·삭제 제공 서비스가 모두 포함된다")
    void findIds_includesDeleted() {
        ServiceOffering deleted = saveDeleted();
        ServiceOffering active = jpaServiceOfferingRepository
                .saveAndFlush(ServiceOffering.of(providerId, UUID.randomUUID(), regionId));

        assertThat(serviceOfferingQueryRepository.findIdsByProviderIdIncludingDeleted(providerId))
                .containsExactlyInAnyOrder(deleted.getId(), active.getId());
    }

    @Test
    @DisplayName("삭제된 제공 서비스는 일반 조회와 매칭 후보에서 계속 제외된다")
    void deleted_excludedFromGeneralQueries() {
        ServiceOffering deleted = saveDeleted();

        assertThat(serviceOfferingQueryRepository.findById(deleted.getId())).isEmpty();
        assertThat(serviceOfferingQueryRepository
                .findAllByRegionIdAndProvideServiceId(regionId, provideServiceId))
                .isEmpty();
    }

    @Test
    @DisplayName("제공자별 제공 서비스 → 제공자 매핑에 삭제 건은 포함하고 다른 제공자는 제외한다")
    void findOfferingProviderIds_includesDeleted() {
        ServiceOffering deleted = saveDeleted();
        ServiceOffering active = jpaServiceOfferingRepository
                .saveAndFlush(ServiceOffering.of(providerId, UUID.randomUUID(), regionId));
        ServiceOffering other = jpaServiceOfferingRepository
                .saveAndFlush(ServiceOffering.of(UUID.randomUUID(), provideServiceId, regionId));

        Map<UUID, UUID> result =
                serviceOfferingQueryRepository.findOfferingProviderIdsIncludingDeleted(List.of(providerId));

        assertThat(result)
                .containsEntry(deleted.getId(), providerId)
                .containsEntry(active.getId(), providerId)
                .doesNotContainKey(other.getId());
    }

    @Test
    @DisplayName("제공자 목록이 비어 있으면 조회하지 않고 빈 매핑을 반환한다")
    void findOfferingProviderIds_emptyProviders() {
        assertThat(serviceOfferingQueryRepository.findOfferingProviderIdsIncludingDeleted(List.of()))
                .isEmpty();
    }
}
