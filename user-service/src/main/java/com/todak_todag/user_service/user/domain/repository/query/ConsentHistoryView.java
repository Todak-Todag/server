package com.todak_todag.user_service.user.domain.repository.query;

import com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentHistoryView(
        UUID consentId,
        UUID consentDocumentId,
        UUID consentDocumentVersionId,
        String title,
        String version,
        ConsentStatus status,
        LocalDateTime agreedAt,
        LocalDateTime withdrawnAt
) {
}