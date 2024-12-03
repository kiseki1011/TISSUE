package com.uranus.taskmanager.api.workspace.presentation.dto.response;

import lombok.Getter;

@Getter
public class DeleteWorkspaceResponse {

	private String workspaceCode;

	public DeleteWorkspaceResponse(
		String workspaceCode
	) {
		this.workspaceCode = workspaceCode;
	}

	public static DeleteWorkspaceResponse from(String workspaceCode) {
		return new DeleteWorkspaceResponse(workspaceCode);
	}
}
