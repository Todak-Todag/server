package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.ConsentDocumentCreateResult;

import java.util.UUID;

public record ConsentDocumentCreateResponse(
        UUID consentDocumentId,
        UUID consentDocumentVersionId
) {

    public static ConsentDocumentCreateResponse from(
            ConsentDocumentCreateResult result
    ) {
        return new ConsentDocumentCreateResponse(
                result.consentDocumentId(),
                result.consentDocumentVersionId()
        );
    }
}