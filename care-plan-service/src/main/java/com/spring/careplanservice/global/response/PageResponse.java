package com.spring.careplanservice.global.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        List<T> content,
        PageInfo pageInfo
) {

    public static <S, T> PageResponse<T> of(
            Page<S> page,
            Function<S, T> mapper
    ) {
        List<T> content = page.getContent()
                .stream()
                .map(mapper)
                .toList();

        return new PageResponse<>(
                content,
                PageInfo.of(page)
        );
    }
}
