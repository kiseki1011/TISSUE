package com.tissue.api.workspace.application.dto.request;

public record UpdateDisplayNameCommand(
	String workspaceKey,
	Long memberId,
	String displayName // TODO: Should I use newDisplayName?
) {
}
