package com.todak_todag.user_service.user.application.command;

import java.util.UUID;

// 지역 수정을 위한 전달 데이터
public record RegionUpdateCommand(
        UUID regionId,
        String province,
        String district,
        String regionCode
) {

    // 변경하려는 데이터를 최소 하나라도 받으려는 로직
    public boolean hasUpdateValue() {
        return province != null
                || district != null
                || regionCode != null;
    }
}