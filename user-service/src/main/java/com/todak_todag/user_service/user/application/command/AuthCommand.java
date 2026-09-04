package com.todak_todag.user_service.user.application.command;

public final class AuthCommand {

	public record AuthLoginCommand(String username, String password) {}
}
