package com.todak_todag.user_service.user.infrastructure.persistence.query;

import com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus;
import com.todak_todag.user_service.user.domain.repository.query.ConsentQueryRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ConsentQueryRepositoryImpl
        implements ConsentQueryRepository {

    private final JpaConsentRepository jpaRepository;

    @Override
    public boolean existsAgreedConsent(
            UUID userId,
            List<UUID> consentDocumentVersionIds
    ) {
        return jpaRepository
                .existsByUserIdAndConsentDocumentVersionIdInAndStatus(
                        userId,
                        consentDocumentVersionIds,
                        ConsentStatus.AGREED
                );
    }
}