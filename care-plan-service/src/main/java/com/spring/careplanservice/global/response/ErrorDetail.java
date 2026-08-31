package com.spring.careplanservice.global.response;

public record ErrorDetail(
        String reason
) {

    public static ErrorDetail of(String reason) {
        return new ErrorDetail(reason);
    }
}