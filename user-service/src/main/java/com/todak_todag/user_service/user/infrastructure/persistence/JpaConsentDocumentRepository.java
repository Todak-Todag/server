package com.todak_todag.user_service.user.infrastructure.persistence;

import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JpaConsentDocumentRepository
        extends JpaRepository<ConsentDocument, UUID> {

    // 논리 삭제 되지 않은 약관 문서 조회
    Optional<ConsentDocument> findByIdAndDeletedAtIsNull(
            UUID consentDocumentId
    );
}