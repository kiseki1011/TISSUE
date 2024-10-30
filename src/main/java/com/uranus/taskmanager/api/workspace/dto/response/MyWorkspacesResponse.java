package com.uranus.taskmanager.api.workspace.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.workspace.dto.WorkspaceDetail;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MyWorkspacesResponse {

	private List<WorkspaceDetail> workspaces = new ArrayList<>();
	private int workspaceCount;

	@Builder
	public MyWorkspacesResponse(List<WorkspaceDetail> workspaces) {
		if (workspaces != null) {
			this.workspaces = workspaces;
		}
		this.workspaceCount = getWorkspaceCount();
	}

	public int getWorkspaceCount() {
		return workspaces.size();
	}

	public static MyWorkspacesResponse from(List<WorkspaceDetail> workspaces) {
		return MyWorkspacesResponse.builder()
			.workspaces(workspaces)
			.build();
	}
}
