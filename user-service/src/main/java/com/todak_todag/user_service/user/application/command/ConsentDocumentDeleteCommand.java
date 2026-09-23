package com.todak_todag.user_service.user.application.command;

import java.util.UUID;

public record ConsentDocumentDeleteCommand(
        UUID consentDocumentId,
        UUID deletedBy
) {
}