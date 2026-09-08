package com.todak_todag.social_worker_service.global.response;

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

    public static <T> ApiResponse<T> ok(
            String message,
            T data
    ) {
        return success(
                200,
                message,
                data
        );
    }

    public static <T> ApiResponse<T> created(
            String message,
            T data
    ) {
        return success(
                201,
                message,
                data
        );
    }

    public static <T> ApiResponse<T> accepted(
            String message,
            T data
    ) {
        return success(
                202,
                message,
                data
        );
    }
}
