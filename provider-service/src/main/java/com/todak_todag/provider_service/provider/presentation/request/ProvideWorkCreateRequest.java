package com.todak_todag.provider_service.provider.presentation.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record ProvideWorkCreateRequest(
        @NotNull(message = "요일은 필수입니다.")
        @Min(value = 1, message = "요일은 1(월)부터 7(일) 사이여야 합니다.")
        @Max(value = 7, message = "요일은 1(월)부터 7(일) 사이여야 합니다.")
        Integer day,

        @NotNull(message = "시작 시각은 필수입니다.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime startedAt,

        @NotNull(message = "종료 시각은 필수입니다.")
        @JsonFormat(pattern = "HH:mm")
        LocalTime finishedAt
) {
}