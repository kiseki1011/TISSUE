package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class GetMyWorkspacesResponse {
	/**
	 * Todo
	 *  - PageInfo 클래스를 만들어서 응답에 포함(totalElements 대신, 어차피 안에 포함 됨)
	 */
	private List<WorkspaceDetail> workspaces = new ArrayList<>();
	private long totalElements;

	@Builder
	public GetMyWorkspacesResponse(List<WorkspaceDetail> workspaces, long totalElements) {
		if (workspaces != null) {
			this.workspaces = workspaces;
		}
		this.totalElements = totalElements;
	}

	public static GetMyWorkspacesResponse from(List<WorkspaceDetail> workspaces, long totalElements) {
		return GetMyWorkspacesResponse.builder()
			.workspaces(workspaces)
			.totalElements(totalElements)
			.build();
	}
}
