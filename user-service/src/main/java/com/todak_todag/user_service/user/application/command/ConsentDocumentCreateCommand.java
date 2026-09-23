package com.todak_todag.user_service.user.application.command;

import com.todak_todag.user_service.user.domain.entity.ConsentDocument.ConsentType;

import java.time.LocalDateTime;

public record ConsentDocumentCreateCommand(
        ConsentType consentType,
        String title,
        boolean isRequired,
        String version,
        String content,
        LocalDateTime effectiveAt
) {
}