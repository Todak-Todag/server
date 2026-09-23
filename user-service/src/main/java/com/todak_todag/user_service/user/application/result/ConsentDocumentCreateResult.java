package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.ConsentDocument;
import com.todak_todag.user_service.user.domain.entity.ConsentDocumentVersion;

import java.util.UUID;

public record ConsentDocumentCreateResult(
        UUID consentDocumentId,
        UUID consentDocumentVersionId
) {

    public static ConsentDocumentCreateResult of(
            ConsentDocument consentDocument,
            ConsentDocumentVersion consentDocumentVersion
    ) {
        return new ConsentDocumentCreateResult(
                consentDocument.getId(),
                consentDocumentVersion.getId()
        );
    }
}