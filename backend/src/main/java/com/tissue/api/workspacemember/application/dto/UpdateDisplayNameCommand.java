package com.tissue.api.workspacemember.application.dto;

public record UpdateDisplayNameCommand(
	String workspaceKey,
	Long memberId,
	String displayName // TODO: Should I use newDisplayName?
) {
}
