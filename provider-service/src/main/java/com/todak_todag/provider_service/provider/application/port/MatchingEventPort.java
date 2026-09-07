package com.todak_todag.provider_service.provider.application.port;

import com.todak_todag.provider_service.provider.application.event.ProviderMatchFailedEvent;
import com.todak_todag.provider_service.provider.application.event.ProviderMatchedEvent;

public interface MatchingEventPort {

    void publishMatched(ProviderMatchedEvent event);

    void publishMatchFailed(ProviderMatchFailedEvent event);
}