package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.ConsentDocumentFindDetailResult;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentDocumentFindDetailResponse(
        UUID consentDocumentId,
        UUID consentDocumentVersionId,
        String consentType,
        String title,
        String version,
        String content,
        boolean isRequired,
        LocalDateTime effectiveAt
) {

    public static ConsentDocumentFindDetailResponse from(
            ConsentDocumentFindDetailResult result
    ) {
        return new ConsentDocumentFindDetailResponse(
                result.consentDocumentId(),
                result.consentDocumentVersionId(),
                result.consentType(),
                result.title(),
                result.version(),
                result.content(),
                result.required(),
                result.effectiveAt()
        );
    }
}