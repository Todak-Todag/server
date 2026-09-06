package com.todak_todag.user_service.user.application.command;

import java.util.List;
import java.util.UUID;

public record ConsentCreateCommand(
        UUID userId,
        List<UUID> consentDocumentVersionIds
) {
}