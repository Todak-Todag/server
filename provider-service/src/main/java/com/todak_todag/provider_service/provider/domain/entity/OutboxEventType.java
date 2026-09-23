package com.todak_todag.provider_service.provider.domain.entity;

// 아웃박스에 적재된 이벤트의 종류
// 릴레이가 이 값을 보고 어떤 페이로드로 되돌릴지 정한다
public enum OutboxEventType {

    PROVIDER_MATCHED,
    PROVIDER_MATCH_FAILED
}