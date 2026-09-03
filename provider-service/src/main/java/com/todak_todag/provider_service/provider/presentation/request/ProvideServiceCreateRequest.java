package com.todak_todag.provider_service.provider.presentation.request;

import com.todak_todag.provider_service.provider.application.command.ProvideServiceCreateCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProvideServiceCreateRequest(

        @NotBlank(message = "서비스명은 필수입니다.")
        @Size(max = 50, message = "서비스명은 최대 50자입니다.")
        String name,

        @NotBlank(message = "서비스 설명은 필수입니다.")
        @Size(max = 255, message = "서비스 설명은 최대 255자입니다.")
        String content
) {

    public ProvideServiceCreateCommand toCommand() {
        return new ProvideServiceCreateCommand(name, content);
    }
}