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
import org.hibernate.annotations.SQLRestriction;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_provide_services")
@SQLRestriction("deleted_at is null")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProvideService extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "provide_service_id")
    private UUID id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "content", nullable = false)
    private String content;

    public static ProvideService of(String name, String content) {
        ProvideService provideService = new ProvideService();
        provideService.name = name;
        provideService.content = content;

        return provideService;
    }
}