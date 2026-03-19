package com.tissue.security.application.dto.command;

public record SignupOAuthMemberCommand(String registerToken, String username, String name) {}
