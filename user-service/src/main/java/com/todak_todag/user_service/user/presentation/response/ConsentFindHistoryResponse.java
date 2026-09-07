package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.ConsentFindHistoryResult;
import com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ConsentFindHistoryResponse(
        List<ConsentHistoryItem> content
) {

    public static ConsentFindHistoryResponse of(
            List<ConsentFindHistoryResult> results
    ) {
        return new ConsentFindHistoryResponse(
                results.stream()
                        .map(ConsentHistoryItem::from)
                        .toList()
        );
    }

    public record ConsentHistoryItem(
            UUID consentId,
            UUID consentDocumentId,
            UUID consentDocumentVersionId,
            String title,
            String version,
            ConsentStatus status,
            LocalDateTime agreedAt,
            LocalDateTime withdrawnAt
    ) {

        public static ConsentHistoryItem from(
                ConsentFindHistoryResult result
        ) {
            return new ConsentHistoryItem(
                    result.consentId(),
                    result.consentDocumentId(),
                    result.consentDocumentVersionId(),
                    result.title(),
                    result.version(),
                    result.status(),
                    result.agreedAt(),
                    result.withdrawnAt()
            );
        }
    }
}