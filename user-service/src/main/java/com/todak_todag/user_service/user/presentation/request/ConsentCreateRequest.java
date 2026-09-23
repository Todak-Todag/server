package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.user.application.command.ConsentCreateCommand;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ConsentCreateRequest(

        @NotEmpty(message = "동의할 약관 버전은 1개 이상이어야 합니다.")
        List<@NotNull(message = "약관 버전 ID는 필수입니다.") UUID>
        consentDocumentVersionIds

) {

    public ConsentCreateCommand toCommand(UUID userId) {
        return new ConsentCreateCommand(
                userId,
                consentDocumentVersionIds
        );
    }
}