package com.uranus.taskmanager.api.invitation.dto.response;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

import lombok.Builder;
import lombok.Getter;

/**
 * Todo: Audit 적용 후 수정, fromEnity() 수정
 */
@Getter
public class InvitationAcceptResponse {
	private final String workspaceCode;
	private final String workspaceName;
	private final String workspaceDescription;
	private final String loginId;
	private final String email;
	private final String nickname;
	private final int headcount;

	@Builder
	public InvitationAcceptResponse(String workspaceCode, String workspaceName, String workspaceDescription,
		String loginId,
		String email, String nickname, int headcount) {
		this.workspaceCode = workspaceCode;
		this.workspaceName = workspaceName;
		this.workspaceDescription = workspaceDescription;
		this.loginId = loginId;
		this.email = email;
		this.nickname = nickname;
		this.headcount = headcount;
	}

	public static InvitationAcceptResponse fromEntity(Invitation invitation, WorkspaceMember workspaceMember) {
		return InvitationAcceptResponse.builder()
			.workspaceCode(invitation.getWorkspace().getCode())
			.workspaceName(invitation.getWorkspace().getName())
			.workspaceDescription(invitation.getWorkspace().getDescription())
			.loginId(invitation.getMember().getLoginId())
			.email(invitation.getMember().getEmail())
			.nickname(workspaceMember.getNickname())
			.headcount(invitation.getWorkspace().getWorkspaceMembers().size())
			.build();
	}
}
