package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.ConsentDocument;

import java.util.UUID;

public record ConsentDocumentUpdateRequiredResult(
        UUID consentDocumentId,
        boolean required
) {

    public static ConsentDocumentUpdateRequiredResult from(
            ConsentDocument consentDocument
    ) {
        return new ConsentDocumentUpdateRequiredResult(
                consentDocument.getId(),
                consentDocument.isRequired()
        );
    }
}