package com.todak_todag.provider_service.provider.presentation.response;

import com.todak_todag.provider_service.provider.application.result.ProvideServiceInfoResult;

import java.util.List;
import java.util.UUID;

public record ProvideServiceInfoListResponse(
        List<ProvideServiceInfo> content
) {

    public static ProvideServiceInfoListResponse from(List<ProvideServiceInfoResult> results) {
        return new ProvideServiceInfoListResponse(
                results.stream().map(ProvideServiceInfo::from).toList()
        );
    }

    public record ProvideServiceInfo(
            UUID provideServiceId,
            String name,
            String content
    ) {

        private static ProvideServiceInfo from(ProvideServiceInfoResult result) {
            return new ProvideServiceInfo(
                    result.provideServiceId(),
                    result.name(),
                    result.content()
            );
        }
    }
}