package com.tissue.api.workspacemember.presentation.dto.response;

import java.util.ArrayList;
import java.util.List;

import com.tissue.api.workspace.presentation.dto.WorkspaceDetail;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class MyWorkspacesResponse {
	/**
	 * Todo
	 *  - PageInfo 클래스를 만들어서 응답에 포함(totalElements 대신, 어차피 안에 포함 됨)
	 */
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
