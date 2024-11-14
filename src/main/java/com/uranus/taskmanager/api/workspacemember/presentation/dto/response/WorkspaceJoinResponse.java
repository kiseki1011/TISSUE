package com.uranus.taskmanager.api.workspacemember.presentation.dto.response;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class WorkspaceJoinResponse {

	/**
	 * Todo
	 *  - WorkspaceDetail에 role 정보가 빠짐
	 *  - role 필드를 추가 -> 서비스 코드 수정
	 */
	private WorkspaceDetail workspaceDetail;
	private String nickname;
	private boolean isAlreadyMember;

	@Builder
	public WorkspaceJoinResponse(WorkspaceDetail workspaceDetail, String nickname,
		boolean isAlreadyMember) {
		this.workspaceDetail = workspaceDetail;
		this.nickname = nickname;
		this.isAlreadyMember = isAlreadyMember;
	}

	public static WorkspaceJoinResponse from(Workspace workspace, WorkspaceMember workspaceMember,
		boolean isAlreadyMember) {
		return WorkspaceJoinResponse.builder()
			.workspaceDetail(WorkspaceDetail.from(workspace, workspaceMember.getRole()))
			.nickname(workspaceMember.getNickname())
			.isAlreadyMember(isAlreadyMember)
			.build();
	}
}
