package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.user.application.command.ConsentDocumentCreateCommand;
import com.todak_todag.user_service.user.domain.entity.ConsentDocument.ConsentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ConsentDocumentCreateRequest(

        @NotNull(message = "약관 유형은 필수입니다.")
        ConsentType consentType,

        @NotBlank(message = "약관 제목은 필수입니다.")
        @Size(max = 255, message = "약관 제목은 최대 255자입니다.")
        String title,

        @NotNull(message = "필수 동의 여부는 필수입니다.")
        Boolean isRequired,

        @NotBlank(message = "약관 버전은 필수입니다.")
        @Size(max = 30, message = "약관 버전은 최대 30자입니다.")
        String version,

        @NotBlank(message = "약관 내용은 필수입니다.")
        String content,

        @NotNull(message = "약관 적용 시작 시점은 필수입니다.")
        LocalDateTime effectiveAt

) {

    public ConsentDocumentCreateCommand toCommand() {
        return new ConsentDocumentCreateCommand(
                consentType,
                title,
                isRequired,
                version,
                content,
                effectiveAt
        );
    }
}