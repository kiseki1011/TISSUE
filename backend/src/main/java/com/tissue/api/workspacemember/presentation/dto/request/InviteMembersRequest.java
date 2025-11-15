package com.tissue.api.workspacemember.presentation.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public record InviteMembersRequest(
	@NotEmpty Set<String> emails
) {
	public static InviteMembersRequest of(Set<String> emails) {
		return new InviteMembersRequest(emails);
	}
}
