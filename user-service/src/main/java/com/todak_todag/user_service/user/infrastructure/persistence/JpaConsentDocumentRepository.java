package com.todak_todag.user_service.user.infrastructure.persistence;

import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaConsentDocumentRepository
        extends JpaRepository<ConsentDocument, UUID> {
}