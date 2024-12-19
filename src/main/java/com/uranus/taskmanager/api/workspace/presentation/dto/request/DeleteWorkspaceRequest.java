package com.uranus.taskmanager.api.workspace.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeleteWorkspaceRequest {

	private String password;

	public DeleteWorkspaceRequest(String password) {
		this.password = password;
	}
}
