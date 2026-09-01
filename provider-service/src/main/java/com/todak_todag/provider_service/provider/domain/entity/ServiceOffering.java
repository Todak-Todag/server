package com.todak_todag.provider_service.provider.domain.entity;

import com.todak_todag.provider_service.global.common.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_provide_service_offerings")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ServiceOffering extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "service_offering_id")
    private UUID id;

    @Column(name = "provider_id", nullable = false, updatable = false)
    private UUID providerId;

    @Column(name = "provide_service_id", nullable = false, updatable = false)
    private UUID provideServiceId;

    @Column(name = "region_id", nullable = false)
    private UUID regionId;

    private ServiceOffering(UUID providerId, UUID provideServiceId, UUID regionId) {
        this.providerId = providerId;
        this.provideServiceId = provideServiceId;
        this.regionId = regionId;
    }

    public static ServiceOffering of(UUID providerId, UUID provideServiceId, UUID regionId) {
        return new ServiceOffering(providerId, provideServiceId, regionId);
    }

    public boolean isOwnedBy(UUID providerId) {
        return this.providerId.equals(providerId);
    }
}