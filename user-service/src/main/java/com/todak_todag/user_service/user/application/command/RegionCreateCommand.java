package com.todak_todag.user_service.user.application.command;

// 요청을 Controller가 Service에 넘기지 않고 Command로 변경하는 DTO
public record RegionCreateCommand(
        String province,
        String district,
        String regionCode
) {
}