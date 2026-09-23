package com.todak_todag.social_worker_service.matching.presentation.request;

import jakarta.validation.constraints.NotBlank;

public record MatchingStatusChangeRequest(

        @NotBlank(message = "변경할 매칭 상태는 필수입니다.")
        String status
) {
}