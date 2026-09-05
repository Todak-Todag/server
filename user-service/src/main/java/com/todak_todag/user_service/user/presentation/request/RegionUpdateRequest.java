package com.todak_todag.user_service.user.presentation.request;

import com.todak_todag.user_service.user.application.command.RegionUpdateCommand;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record RegionUpdateRequest(

        @Size(max = 20, message = "시/도는 최대 20자입니다.")
        String province,

        @Size(max = 20, message = "시/군/구는 최대 20자입니다.")
        String district,

        @Size(max = 20, message = "행정구역 코드는 최대 20자입니다.")
        String regionCode
) {

    public RegionUpdateCommand toCommand(UUID regionId) {
        return new RegionUpdateCommand(
                regionId,
                province,
                district,
                regionCode
        );
    }
}