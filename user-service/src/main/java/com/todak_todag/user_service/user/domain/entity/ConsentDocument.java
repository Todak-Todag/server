package com.todak_todag.user_service.user.domain.entity;

import com.todak_todag.user_service.global.common.BaseAuditableEntity;
import jakarta.persistence.*;
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
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 30)
    private ConsentType consentType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    // 회원가입 시 필수 동의 여부를 판단하는 기준
    @Column(name = "is_required", nullable = false)
    private boolean required;

    // 약관 필수/선택 여부 변경
    public void updateRequired(boolean required) {
        this.required = required;
    }

    public enum ConsentType {
        PERSONAL_INFORMATION,
        SENSITIVE_INFORMATION,
        MARKETING_INFORMATION
    }
}