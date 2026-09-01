package com.todak_todag.schedule_service.global.common;

import java.util.UUID;

// 요청 주체가 특정 사용자가 아닌 경우 created_by/updated_by(NOT NULL)에 기록할 고정 시스템 식별자
public final class SystemId {

    public static final UUID SYSTEM_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private SystemId() {}
}
