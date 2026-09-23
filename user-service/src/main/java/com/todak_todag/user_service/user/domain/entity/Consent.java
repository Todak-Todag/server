package com.todak_todag.user_service.user.domain.entity;

import com.todak_todag.user_service.global.common.BaseUpdatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_consents")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Consent extends BaseUpdatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "consent_id", nullable = false)
    private UUID id;

    // 논리 FK: p_users.user_id
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // 논리 FK: p_consent_document_versions.consent_document_version_id
    @Column(
            name = "consent_document_version_id",
            nullable = false
    )
    private UUID consentDocumentVersionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConsentStatus status;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    public static Consent agree(
            UUID userId,
            UUID consentDocumentVersionId,
            LocalDateTime agreedAt
    ) {
        Consent consent = new Consent();

        consent.userId = userId;
        consent.consentDocumentVersionId =
                consentDocumentVersionId;
        consent.status = ConsentStatus.AGREED;
        consent.agreedAt = agreedAt;

        return consent;
    }

    // 동의 이력을 삭제하지 않고 철회 상태와 시각을 기록
    public void withdraw(
            LocalDateTime withdrawnAt
    ) {
        this.status = ConsentStatus.WITHDRAWN;
        this.withdrawnAt = withdrawnAt;
    }

    public enum ConsentStatus {
        AGREED,
        WITHDRAWN
    }
}