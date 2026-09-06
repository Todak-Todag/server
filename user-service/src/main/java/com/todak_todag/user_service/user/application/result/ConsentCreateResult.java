package com.todak_todag.user_service.user.application.result;

import com.todak_todag.user_service.user.domain.entity.Consent;

import java.util.List;
import java.util.UUID;

public record ConsentCreateResult(
        List<UUID> consentIds
) {

    public static ConsentCreateResult from(
            List<Consent> consents
    ) {
        return new ConsentCreateResult(
                consents.stream()
                        .map(Consent::getId)
                        .toList()
        );
    }
}