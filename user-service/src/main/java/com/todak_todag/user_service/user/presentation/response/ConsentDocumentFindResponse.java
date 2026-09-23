package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.ConsentDocumentFindResult;

import java.util.UUID;

public record ConsentDocumentFindResponse(
        UUID consentDocumentId,
        UUID consentDocumentVersionId,
        String consentType,
        String title,
        String version,
        boolean isRequired
) {

    public static ConsentDocumentFindResponse from(
            ConsentDocumentFindResult result
    ) {
        return new ConsentDocumentFindResponse(
                result.consentDocumentId(),
                result.consentDocumentVersionId(),
                result.consentType(),
                result.title(),
                result.version(),
                result.required()
        );
    }
}