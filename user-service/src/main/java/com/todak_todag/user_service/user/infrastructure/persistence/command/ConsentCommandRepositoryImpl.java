package com.todak_todag.user_service.user.infrastructure.persistence.command;

import com.todak_todag.user_service.user.domain.entity.Consent;
import com.todak_todag.user_service.user.domain.repository.command.ConsentCommandRepository;
import com.todak_todag.user_service.user.infrastructure.persistence.JpaConsentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ConsentCommandRepositoryImpl
        implements ConsentCommandRepository {

    private final JpaConsentRepository jpaRepository;

    @Override
    public List<Consent> saveAll(
            List<Consent> consents
    ) {
        return jpaRepository.saveAll(consents);
    }
}