package com.todak_todag.user_service.user.domain.entity;

import com.todak_todag.user_service.global.common.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Entity
@Table(name = "p_regions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Region extends BaseAuditableEntity {

    // 지역 식별자
    @Id
    @Column(name = "region_id", nullable = false)
    private UUID id;

    // 시/도
    @Column(name = "province", nullable = false, length = 20)
    private String province;

    // 시/군/구
    @Column(name = "district", nullable = false, length = 20)
    private String district;

    // 행정구역 코드
    @Column(name = "region_code", nullable = false, length = 20)
    private String regionCode;

    // 서비스 지원 여부
    @Column(name = "is_active", nullable = false)
    private boolean active;
}