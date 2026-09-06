package com.todak_todag.user_service.user.infrastructure.persistence;

import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaConsentDocumentVersionRepository
        extends JpaRepository<ConsentDocumentVersion, UUID> {

    // 동일 약관 내에 버전 중복 여부 검증
    boolean existsByConsentDocumentIdAndVersion(
            UUID consentDocumentId,
            String version
    );
}