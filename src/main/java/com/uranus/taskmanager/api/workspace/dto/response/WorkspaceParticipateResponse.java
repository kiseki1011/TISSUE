package com.uranus.taskmanager.api.workspace.dto.response;

import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;
import lombok.Getter;

@Getter
public class WorkspaceParticipateResponse {

	private final String name;
	private final String description;
	private final String code;
	private final int headcount;
	private final String nickname;
	private final WorkspaceRole myRole;
	private final boolean isAlreadyMember;

	@Builder
	public WorkspaceParticipateResponse(String name, String description, String code, int headcount, String nickname,
		WorkspaceRole myRole, boolean isAlreadyMember) {
		this.name = name;
		this.description = description;
		this.code = code;
		this.headcount = headcount;
		this.nickname = nickname;
		this.myRole = myRole;
		this.isAlreadyMember = isAlreadyMember;
	}

	public static WorkspaceParticipateResponse from(Workspace workspace, WorkspaceMember workspaceMember,
		int headcount, boolean isAlreadyMember) {
		return WorkspaceParticipateResponse.builder()
			.name(workspace.getName())
			.description(workspace.getDescription())
			.code(workspace.getCode())
			.headcount(headcount)
			.nickname(workspaceMember.getNickname())
			.myRole(workspaceMember.getRole())
			.isAlreadyMember(isAlreadyMember)
			.build();
	}
}
