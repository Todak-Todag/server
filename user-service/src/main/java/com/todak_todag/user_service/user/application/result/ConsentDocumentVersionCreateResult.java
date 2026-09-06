package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;

import java.util.UUID;

public record ConsentDocumentVersionCreateResult(
        UUID consentDocumentVersionId
) {

    public static ConsentDocumentVersionCreateResult from(
            ConsentDocumentVersion consentDocumentVersion
    ) {
        return new ConsentDocumentVersionCreateResult(
                consentDocumentVersion.getId()
        );
    }
}