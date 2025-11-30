package com.tissue.api.workspace.application.dto.request;

import java.util.Set;

public record InviteMembersCommand(
	String workspaceKey,
	Set<String> emails
) {
}
