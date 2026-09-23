package com.spring.careplanservice.careplan.application.port;

import com.spring.careplanservice.careplan.application.result.ProvideServiceInfoResult;

import java.util.List;
import java.util.UUID;

public interface ProviderServiceQueryPort {
    List<ProvideServiceInfoResult> findAllByIds(List<UUID> provideServiceIds);
}
