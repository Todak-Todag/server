package com.todak_todag.user_service.user.application.query;

public record RegionFindAdminQuery(
        Integer page,
        Integer size,
        String province,
        String district,
        String regionCode,
        Boolean isActive
) {
}