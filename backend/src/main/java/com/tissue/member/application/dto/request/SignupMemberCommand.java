package com.tissue.member.application.dto.request;

import lombok.Builder;

@Builder
public record SignupMemberCommand(
	String email,
	String username,
	String password,
	String name
) {
}
