package com.todak_todag.user_service.user.domain.repository.command;

import com.todak_todag.user_service.user.domain.entity.Region;

public interface RegionCommandRepository {

    // 지역 등록
    Region save(Region region);
}