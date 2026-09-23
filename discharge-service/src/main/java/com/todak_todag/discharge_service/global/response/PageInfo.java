package com.todak_todag.discharge_service.global.response;

import org.springframework.data.domain.Page;

public record PageInfo(
        String paginationType,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    private static final String PAGINATION_TYPE = "OFFSET";

    public static PageInfo of(Page<?> page) {
        return new PageInfo(
                PAGINATION_TYPE,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}