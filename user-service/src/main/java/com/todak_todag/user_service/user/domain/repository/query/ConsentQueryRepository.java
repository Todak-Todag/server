package com.todak_todag.user_service.user.domain.repository.query;

import java.util.List;
import java.util.UUID;

public interface ConsentQueryRepository {

    // 사용자가 이미 동의한 약관이 있는지 확인
    boolean existsAgreedConsent(
            UUID userId,
            List<UUID> consentDocumentVersionIds
    );
}