package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.ConsentDocumentVersionCreateResult;

import java.util.UUID;

public record ConsentDocumentVersionCreateResponse(
        UUID consentDocumentVersionId
) {

    public static ConsentDocumentVersionCreateResponse from(
            ConsentDocumentVersionCreateResult result
    ) {
        return new ConsentDocumentVersionCreateResponse(
                result.consentDocumentVersionId()
        );
    }
}