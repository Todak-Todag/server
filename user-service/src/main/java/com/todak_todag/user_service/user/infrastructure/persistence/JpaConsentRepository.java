package com.todak_todag.user_service.user.infrastructure.persistence;

import com.todak_todag.user_service.user.domain.entity.Consent;
import com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JpaConsentRepository
        extends JpaRepository<Consent, UUID> {

    boolean existsByUserIdAndConsentDocumentVersionIdInAndStatus(
            UUID userId,
            Collection<UUID> consentDocumentVersionIds,
            ConsentStatus status
    );

    long countByUserIdAndConsentDocumentVersionIdInAndStatus(
            UUID userId,
            List<UUID> consentDocumentVersionIds,
            Consent.ConsentStatus status
    );
}