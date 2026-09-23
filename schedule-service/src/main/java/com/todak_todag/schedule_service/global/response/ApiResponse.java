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

    // 접수만 하고 결과는 비동기로 확정되는 요청
    public static <T> ApiResponse<T> accepted(String message, T data) {
        return of(202, message, data);
    }
}
