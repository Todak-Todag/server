package com.todak_todag.user_service.user.infrastructure.persistence.query;

import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;
import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentQueryRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ConsentDocumentQueryRepositoryImpl
        implements ConsentDocumentQueryRepository {

    private final JpaConsentDocumentRepository jpaRepo;

    @Override
    public List<ConsentDocumentCurrentView> findAllCurrent(
            LocalDateTime now
    ) {
        return jpaRepo.findAllCurrent(now);
    }

    @Override
    public List<ConsentDocumentCurrentView> findAllCurrentByVersionIds(
            List<UUID> consentDocumentVersionIds,
            LocalDateTime now
    ) {
        return jpaRepo.findAllCurrentByVersionIds(
                consentDocumentVersionIds,
                now
        );
    }
}