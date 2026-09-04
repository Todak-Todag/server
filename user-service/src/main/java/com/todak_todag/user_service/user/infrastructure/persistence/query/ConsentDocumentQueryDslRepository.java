package com.todak_todag.user_service.user.infrastructure.persistence.query;

import com.todak_todag.user_service.user.domain.repository.query.ConsentDocumentCurrentView;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ConsentDocumentQueryDslRepository {

    // 현재 적용 중인 전체 약관 조회
    List<ConsentDocumentCurrentView> findAllCurrent(
            LocalDateTime now
    );

    // 현재 적용 중인 특정 약관 버전 조회
    List<ConsentDocumentCurrentView> findAllCurrentByVersionIds(
            List<UUID> consentDocumentVersionIds,
            LocalDateTime now
    );
}