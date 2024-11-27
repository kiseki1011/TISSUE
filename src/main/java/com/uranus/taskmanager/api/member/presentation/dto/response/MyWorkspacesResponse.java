package com.uranus.taskmanager.api.member.presentation.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MyWorkspacesResponse {

	private List<WorkspaceDetail> workspaces = new ArrayList<>();
	private long totalElements;

	@Builder
	public MyWorkspacesResponse(List<WorkspaceDetail> workspaces, long totalElements) {
		if (workspaces != null) {
			this.workspaces = workspaces;
		}
		this.totalElements = totalElements;
	}

	public static MyWorkspacesResponse from(List<WorkspaceDetail> workspaces, long totalElements) {
		return MyWorkspacesResponse.builder()
			.workspaces(workspaces)
			.totalElements(totalElements)
			.build();
	}
}
