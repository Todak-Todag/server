package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.user.application.command.ConsentDocumentVersionCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConsentDocumentVersionCreateRequest(

        @NotBlank(message = "약관 버전은 필수입니다.")
        @Size(
                max = 30,
                message = "약관 버전은 최대 30자입니다."
        )
        String version,

        @NotBlank(message = "약관 내용은 필수입니다.")
        String content,

        @NotNull(message = "약관 적용 시작 시점은 필수입니다.")
        LocalDateTime effectiveAt
) {

    public ConsentDocumentVersionCreateCommand toCommand(
            UUID consentDocumentId
    ) {
        return new ConsentDocumentVersionCreateCommand(
                consentDocumentId,
                version,
                content,
                effectiveAt
        );
    }
}