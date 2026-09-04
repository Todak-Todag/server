package com.todak_todag.user_service.user.domain.entity;

import com.todak_todag.user_service.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@Table(name = "p_consent_document_versions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConsentDocumentVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "consent_document_version_id", nullable = false)
    private UUID id;

    // 논리 FK: p_consent_documents.consent_document_id
    @Column(name = "consent_document_id", nullable = false)
    private UUID consentDocumentId;

    @Column(name = "version", nullable = false, length = 30)
    private String version;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "effective_at", nullable = false)
    private LocalDateTime effectiveAt;
}