package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.user.application.query.RegionFindAdminQuery;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record RegionFindAdminRequest(

        Integer page,

        Integer size,

        @Size(max = 20)
        String province,

        @Size(max = 20)
        String district,

        @Size(max = 20)
        String regionCode,

        Boolean isActive
) {

    public RegionFindAdminQuery toQuery() {
        return new RegionFindAdminQuery(
                page,
                size,
                province,
                district,
                regionCode,
                isActive
        );
    }
}