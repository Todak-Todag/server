package com.todak_todag.user_service.user.presentation.response;

import com.todak_todag.user_service.user.application.result.ConsentCreateResult;

import java.util.List;
import java.util.UUID;

public record ConsentCreateResponse(
        List<UUID> consentIds
) {

    public static ConsentCreateResponse from(
            ConsentCreateResult result
    ) {
        return new ConsentCreateResponse(
                result.consentIds()
        );
    }
}