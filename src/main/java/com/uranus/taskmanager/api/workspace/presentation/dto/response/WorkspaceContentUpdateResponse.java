package com.uranus.taskmanager.api.workspace.presentation.dto.response;

import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceUpdateDetail;

import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class WorkspaceContentUpdateResponse {
	/**
	 * Todo
	 * 	- 과연 original을 보여줄 필요가 있을까?
	 * 	- 그냥 WorkspaceUpdateDetail을 응답의 데이터로 주는 것을 고려해보자
	 */
	private WorkspaceUpdateDetail original;
	private WorkspaceUpdateDetail updatedTo;

	public WorkspaceContentUpdateResponse(WorkspaceUpdateDetail original, WorkspaceUpdateDetail updatedTo) {
		this.original = original;
		this.updatedTo = updatedTo;
	}

	public static WorkspaceContentUpdateResponse from(WorkspaceUpdateDetail original, WorkspaceUpdateDetail updatedTo) {
		return new WorkspaceContentUpdateResponse(original, updatedTo);
	}
}
