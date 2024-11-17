package com.uranus.taskmanager.api.workspace.presentation.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WorkspaceDeleteRequest {

	private String password;

	public WorkspaceDeleteRequest(String password) {
		this.password = password;
	}
}
