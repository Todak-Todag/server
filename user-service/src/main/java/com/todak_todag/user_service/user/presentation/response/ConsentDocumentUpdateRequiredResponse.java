package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.ConsentDocumentUpdateRequiredResult;

import java.util.UUID;

public record ConsentDocumentUpdateRequiredResponse(
        UUID consentDocumentId,
        boolean isRequired
) {

    public static ConsentDocumentUpdateRequiredResponse from(
            ConsentDocumentUpdateRequiredResult result
    ) {
        return new ConsentDocumentUpdateRequiredResponse(
                result.consentDocumentId(),
                result.required()
        );
    }
}