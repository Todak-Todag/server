package com.spring.careplanservice.global.response;

public record ApiResponse<T>(
        boolean success,
        int code,
        String message,
        T data
) {

    public static <T> ApiResponse<T> success(
            int code,
            String message,
            T data
    ) {
        return new ApiResponse<>(
                true,
                code,
                message,
                data
        );
    }
}