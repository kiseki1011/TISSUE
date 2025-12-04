package com.tissue.api.workspace.application.dto.request;

public record ExpireLinkCommand(
	String workspaceKey,
	String token
) {
}
