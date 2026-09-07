package com.todak_todag.user_service.user.application.command;

import java.util.UUID;

public record ConsentWithdrawCommand(
        UUID userId,
        UUID consentId
) {
}