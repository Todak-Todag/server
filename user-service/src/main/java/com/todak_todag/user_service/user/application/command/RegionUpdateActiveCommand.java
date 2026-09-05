package com.todak_todag.user_service.user.application.command;

import java.util.UUID;

// 지역 상태 변경을 위한
public record RegionUpdateActiveCommand(
        UUID regionId,
        boolean isActive
) {
}