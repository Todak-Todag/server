package com.todak_todag.provider_service.provider.application.command;

public record ProvideServiceCreateCommand(
        String name,
        String content
) {
}