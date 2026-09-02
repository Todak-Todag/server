package com.todak_todag.user_service.user.application.service.query;

import com.todak_todag.user_service.user.application.result.RegionFindAvailableResult;
import com.todak_todag.user_service.user.domain.repository.query.RegionQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegionQueryService {

    private final RegionQueryRepository regionQueryRepository;

    public List<RegionFindAvailableResult> findAvailableRegions() {
        // 서비스 가능 지역 조회
        return regionQueryRepository.findAllAvailableRegions()
                .stream()
                .map(RegionFindAvailableResult::from)
                .toList();
    }
}