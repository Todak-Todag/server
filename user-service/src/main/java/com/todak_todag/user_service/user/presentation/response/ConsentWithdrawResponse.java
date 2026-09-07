package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.ConsentWithdrawResult;
import com.todak_todag.user_service.user.domain.entity.Consent.ConsentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentWithdrawResponse(
        UUID consentId,
        ConsentStatus status,
        LocalDateTime withdrawnAt
) {

    public static ConsentWithdrawResponse from(
            ConsentWithdrawResult result
    ) {
        return new ConsentWithdrawResponse(
                result.consentId(),
                result.status(),
                result.withdrawnAt()
        );
    }
}