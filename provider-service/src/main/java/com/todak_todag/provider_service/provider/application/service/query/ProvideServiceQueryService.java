package com.todak_todag.provider_service.provider.application.service.query;

import com.todak_todag.provider_service.provider.application.result.ProvideServiceSearchResult;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProvideServiceQueryService {

    private final ProvideServiceQueryRepository provideServiceQueryRepository;

    public Page<ProvideServiceSearchResult> search(Pageable pageable) {
        return provideServiceQueryRepository.findAll(pageable)
                .map(ProvideServiceSearchResult::from);
    }
}