package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus;
import com.todak_todag.user_service.user.domain.repository.query.ConsentHistoryView;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentFindHistoryResult(
        UUID consentId,
        UUID consentDocumentId,
        UUID consentDocumentVersionId,
        String title,
        String version,
        ConsentStatus status,
        LocalDateTime agreedAt,
        LocalDateTime withdrawnAt
) {

    public static ConsentFindHistoryResult from(
            ConsentHistoryView view
    ) {
        return new ConsentFindHistoryResult(
                view.consentId(),
                view.consentDocumentId(),
                view.consentDocumentVersionId(),
                view.title(),
                view.version(),
                view.status(),
                view.agreedAt(),
                view.withdrawnAt()
        );
    }
}