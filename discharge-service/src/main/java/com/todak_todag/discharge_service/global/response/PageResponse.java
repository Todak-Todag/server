package com.todak_todag.discharge_service.global.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        PageInfo pageInfo
) {

    public static <T> PageResponse<T> of(
            Page<T> page
    ) {
        return new PageResponse<>(
                page.getContent(),
                PageInfo.of(page)
        );
    }

    public static <T, R> PageResponse<R> of(
            Page<T> page,
            List<R> content
    ) {
        return new PageResponse<>(
                content,
                PageInfo.of(page)
        );
    }
}