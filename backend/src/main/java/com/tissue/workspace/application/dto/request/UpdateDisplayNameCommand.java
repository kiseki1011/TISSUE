package com.tissue.workspace.application.dto.request;

public record UpdateDisplayNameCommand(
	String workspaceKey,
	Long actorMemberId,
	String displayName
) {
}
