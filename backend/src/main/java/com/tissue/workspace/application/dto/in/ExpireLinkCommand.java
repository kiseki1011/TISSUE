package com.tissue.workspace.application.dto.in;

public record ExpireLinkCommand(
	String workspaceKey,
	String token
) {
}
