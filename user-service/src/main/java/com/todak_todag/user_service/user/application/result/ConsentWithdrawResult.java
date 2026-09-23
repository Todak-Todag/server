package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentWithdrawResult(
        UUID consentId,
        ConsentStatus status,
        LocalDateTime withdrawnAt
) {
}