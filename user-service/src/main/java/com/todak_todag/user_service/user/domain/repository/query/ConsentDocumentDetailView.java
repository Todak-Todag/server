package com.todak_todag.user_service.user.domain.repository.query;

import java.time.LocalDateTime;
import java.util.UUID;

// 상세 조회 view
public record ConsentDocumentDetailView(
        UUID consentDocumentId,
        UUID consentDocumentVersionId,
        String consentType,
        String title,
        String version,
        String content,
        boolean isRequired,
        LocalDateTime effectiveAt
) {
}