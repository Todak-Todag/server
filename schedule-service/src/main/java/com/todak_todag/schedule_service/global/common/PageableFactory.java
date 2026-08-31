package com.todak_todag.schedule_service.global.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableFactory {

    private static final Set<Integer> ALLOWED_SIZES = Set.of(10, 30, 50);
    private static final int DEFAULT_SIZE = 10;
    private static final String SORT_PROPERTY = "createdAt";
    private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.DESC;

    private PageableFactory() {
    }

    public static Pageable of(Integer page, Integer size, String sort) {
        int resolvedPage = resolvePage(page);
        int resolvedSize = resolveSize(size);
        Sort resolvedSort = resolveSort(sort);
        return PageRequest.of(resolvedPage, resolvedSize, resolvedSort);
    }

    private static int resolvePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private static int resolveSize(Integer size) {
        if (size == null || !ALLOWED_SIZES.contains(size)) {
            return DEFAULT_SIZE;
        }
        return size;
    }

    private static Sort resolveSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(DEFAULT_DIRECTION, SORT_PROPERTY);
        }

        String[] parts = sort.split(",", 2);
        Sort.Direction direction = DEFAULT_DIRECTION;
        if (parts.length > 1) {
            try {
                direction = Sort.Direction.fromString(parts[1].trim());
            } catch (IllegalArgumentException ignored) {
                direction = DEFAULT_DIRECTION;
            }
        }
        // 정렬 가능 필드는 현재 createdAt(최신순/오래된순)만 지원한다.
        return Sort.by(direction, SORT_PROPERTY);
    }
}
