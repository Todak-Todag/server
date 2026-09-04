package com.todak_todag.user_service.user.domain.entity;

import com.todak_todag.user_service.global.common.BaseAuditableEntity;
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
@Table(name = "p_consent_documents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsentDocument extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "consent_document_id", nullable = false)
    private UUID id;

    // 개인정보, 민감정보 등 약관의 종류
    @Column(name = "consent_type", nullable = false, length = 30)
    private String consentType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    // 회원가입 시 필수 동의 여부를 판단하는 기준
    @Column(name = "is_required", nullable = false)
    private boolean required;
}