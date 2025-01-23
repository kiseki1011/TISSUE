package com.tissue.api.workspacemember.presentation.dto.request;

import java.util.Set;

import jakarta.validation.constraints.NotEmpty;

public record InviteMembersRequest(

	@NotEmpty(message = "{valid.notempty.memberidentifiers}")
	Set<String> memberIdentifiers
) {
	public static InviteMembersRequest of(Set<String> memberIdentifiers) {
		return new InviteMembersRequest(memberIdentifiers);
	}
}
