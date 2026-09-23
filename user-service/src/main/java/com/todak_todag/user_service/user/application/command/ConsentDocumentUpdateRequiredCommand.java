package com.todak_todag.user_service.user.application.command;

import java.util.UUID;

public record ConsentDocumentUpdateRequiredCommand(
        UUID consentDocumentId,
        boolean isRequired
) {
}