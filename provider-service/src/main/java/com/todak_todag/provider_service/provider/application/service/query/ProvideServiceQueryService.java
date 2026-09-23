package com.todak_todag.provider_service.provider.application.service.query;

import com.todak_todag.provider_service.provider.application.result.ProvideServiceInfoResult;
import com.todak_todag.provider_service.provider.application.result.ProvideServiceSearchResult;
import com.todak_todag.provider_service.provider.domain.repository.query.ProvideServiceQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProvideServiceQueryService {

    private final ProvideServiceQueryRepository provideServiceQueryRepository;

    public Page<ProvideServiceSearchResult> search(Pageable pageable) {
        return provideServiceQueryRepository.findAll(pageable)
                .map(ProvideServiceSearchResult::from);
    }

    // 요청한 ID 중 존재하는 서비스 종류만 반환한다
    // 누락된 ID의 처리는 호출하는 서비스가 담당한다
    public List<ProvideServiceInfoResult> findAllByIds(List<UUID> provideServiceIds) {
        return provideServiceQueryRepository.findAllByIdIn(provideServiceIds).stream()
                .map(ProvideServiceInfoResult::from)
                .toList();
    }
}