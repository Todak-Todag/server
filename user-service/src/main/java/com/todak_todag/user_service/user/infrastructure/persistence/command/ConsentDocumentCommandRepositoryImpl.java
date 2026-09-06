package com.todak_todag.user_service.user.infrastructure.persistence.command;

import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.repository.command.ConsentDocumentCommandRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConsentDocumentCommandRepositoryImpl
        implements ConsentDocumentCommandRepository {

    private final JpaConsentDocumentRepository jpaRepository;

    @Override
    public ConsentDocument save(
            ConsentDocument consentDocument
    ) {
        return jpaRepository.save(consentDocument);
    }
}