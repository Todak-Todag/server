package com.todak_todag.api_gateway.controller;

public record TokenRequest(
		String caller, String audience
) {

}
