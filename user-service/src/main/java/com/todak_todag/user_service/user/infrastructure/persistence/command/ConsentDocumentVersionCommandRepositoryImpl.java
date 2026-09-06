package com.todak_todag.user_service.user.infrastructure.persistence.command;

import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;
import com.todak_todag.user_service.user.domain.repository.command.ConsentDocumentVersionCommandRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentDocumentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConsentDocumentVersionCommandRepositoryImpl
        implements ConsentDocumentVersionCommandRepository {

    private final JpaConsentDocumentVersionRepository jpaRepository;

    @Override
    public ConsentDocumentVersion save(
            ConsentDocumentVersion consentDocumentVersion
    ) {
        return jpaRepository.save(consentDocumentVersion);
    }
}