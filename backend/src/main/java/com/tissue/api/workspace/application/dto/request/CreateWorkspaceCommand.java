package com.tissue.api.workspace.application.dto.request;

public record CreateWorkspaceCommand(
	String name,
	String description,
	Long memberId
) {
}
