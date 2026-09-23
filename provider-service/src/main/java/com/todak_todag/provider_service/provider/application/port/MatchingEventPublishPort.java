package com.todak_todag.provider_service.provider.application.port;

import com.todak_todag.provider_service.provider.application.event.ProviderMatchFailedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchedEvent;

// 아웃박스 릴레이가 실제로 브로커에 내보낼 때 쓰는 포트
// MatchingEventPort는 아웃박스 적재까지만 책임진다
public interface MatchingEventPublishPort {

    void publishMatched(ProviderMatchedEvent event);

    void publishMatchFailed(ProviderMatchFailedEvent event);
}