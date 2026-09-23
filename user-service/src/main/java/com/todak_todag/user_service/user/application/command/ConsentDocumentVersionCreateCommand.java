package com.todak_todag.user_service.user.application.command;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentDocumentVersionCreateCommand(
        UUID consentDocumentId,
        String version,
        String content,
        LocalDateTime effectiveAt
) {
}