package com.uranus.taskmanager.api.workspace.dto.response;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class WorkspaceParticipateResponse {

	private WorkspaceDetail workspaceDetail;
	// Todo: Workspace에 headcount 필드 추가 -> WorkspaceDetail에 headcount 추가
	//  -> WorkspaceParticipateResponse의 headcount 제거
	private int headcount;
	private String nickname;
	private boolean isAlreadyMember;

	@Builder
	public WorkspaceParticipateResponse(WorkspaceDetail workspaceDetail, int headcount, String nickname,
		boolean isAlreadyMember) {
		this.workspaceDetail = workspaceDetail;
		this.headcount = headcount;
		this.nickname = nickname;
		this.isAlreadyMember = isAlreadyMember;
	}

	public static WorkspaceParticipateResponse from(Workspace workspace, WorkspaceMember workspaceMember,
		int headcount, boolean isAlreadyMember) {
		return WorkspaceParticipateResponse.builder()
			.workspaceDetail(WorkspaceDetail.from(workspace, workspaceMember.getRole()))
			.headcount(headcount)
			.nickname(workspaceMember.getNickname())
			.isAlreadyMember(isAlreadyMember)
			.build();
	}
}
