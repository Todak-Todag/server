package com.todak_todag.provider_service.provider.infrastructure.persistence;

import com.todak_todag.provider_service.provider.domain.entity.ServiceOffering;
import com.todak_todag.provider_service.support.ContainerTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("제공 서비스 중복 제약 통합")
class ServiceOfferingUniqueIntegrationTest extends ContainerTestSupport {

    @Autowired
    private JpaServiceOfferingRepository jpaServiceOfferingRepository;

    private final UUID providerId = UUID.randomUUID();
    private final UUID provideServiceId = UUID.randomUUID();
    private final UUID regionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jpaServiceOfferingRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 제공자·서비스 종류 조합은 DB가 두 번 저장하지 못하게 막는다")
    void save_duplicate() {
        jpaServiceOfferingRepository.saveAndFlush(ServiceOffering.of(providerId, provideServiceId, regionId));

        assertThatThrownBy(() -> jpaServiceOfferingRepository
                .saveAndFlush(ServiceOffering.of(providerId, provideServiceId, regionId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("논리 삭제된 건이 있으면 같은 조합을 다시 등록할 수 있다")
    void save_afterSoftDelete() {
        ServiceOffering deleted = jpaServiceOfferingRepository
                .saveAndFlush(ServiceOffering.of(providerId, provideServiceId, regionId));
        deleted.markDeleted(providerId);
        jpaServiceOfferingRepository.saveAndFlush(deleted);

        assertThatCode(() -> jpaServiceOfferingRepository
                .saveAndFlush(ServiceOffering.of(providerId, provideServiceId, regionId)))
                .doesNotThrowAnyException();
    }
}
