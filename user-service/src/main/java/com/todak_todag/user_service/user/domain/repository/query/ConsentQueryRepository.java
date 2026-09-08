package com.todak_todag.user_service.user.domain.repository.query;

import java.util.List;
import java.util.UUID;

public interface ConsentQueryRepository {

    // 사용자가 이미 동의한 약관이 있는지 확인
    boolean existsAgreedConsent(
            UUID userId,
            List<UUID> consentDocumentVersionIds
    );

    // 사용자의 약관 동의 및 철회 이력 조회
    List<ConsentHistoryView> findAllByUserId(UUID userId);

    // 필수 약관 몇 개 동의 했는지 조회
    long countAgreedConsents(
            UUID userId,
            List<UUID> consentDocumentVersionIds
    );
}