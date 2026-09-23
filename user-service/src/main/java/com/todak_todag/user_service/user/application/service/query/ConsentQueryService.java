package com.todak_todag.user_service.user.application.service.query;

import com.todak_todag.user_service.user.application.result.ConsentFindHistoryResult;
import com.todak_todag.user_service.user.domain.repository.query.ConsentQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConsentQueryService {

    private final ConsentQueryRepository consentQueryRepository;

    public List<ConsentFindHistoryResult> findMyConsents(
            UUID userId
    ) {
        return consentQueryRepository
                .findAllByUserId(userId)
                .stream()
                .map(ConsentFindHistoryResult::from)
                .toList();
    }
}