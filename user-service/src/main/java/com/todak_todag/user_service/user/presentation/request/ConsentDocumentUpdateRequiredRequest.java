package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.user.application.command.ConsentDocumentUpdateRequiredCommand;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ConsentDocumentUpdateRequiredRequest(

        //boolean으로 할 경우 기본값 false가 적용될 수도 있음.
        //Boolean은 true, false, null이 가능.
        @NotNull(message = "필수 동의 여부는 필수입니다.")
        Boolean isRequired

) {

    public ConsentDocumentUpdateRequiredCommand toCommand(
            UUID consentDocumentId
    ) {
        return new ConsentDocumentUpdateRequiredCommand(
                consentDocumentId,
                isRequired
        );
    }
}