package com.tissue.workspace.application.dto.request;

public record ExpireLinkCommand(
	String workspaceKey,
	String token
) {
}
