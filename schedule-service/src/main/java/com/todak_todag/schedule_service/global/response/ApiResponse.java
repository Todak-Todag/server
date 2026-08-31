package com.todak_todag.schedule_service.global.response;

public record ApiResponse<T>(
        boolean success,
        int code,
        String message,
        T data

) {

    public static <T> ApiResponse<T> of(int code, String message, T data) {
        return new ApiResponse<T>(true, code, message, data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return of(200, message, data);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return of(201, message, data);
    }
}
