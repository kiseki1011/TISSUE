package com.tissue.api.workspace.application.dto.request;

public record JoinViaLinkCommand(
	String workspaceKey,
	String token,
	Long memberId
) {
}
