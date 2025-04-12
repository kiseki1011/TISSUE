package com.tissue.api.workspace.domain.event;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.vo.EntityReference;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import lombok.Getter;

@Getter
public class MemberJoinedWorkspaceEvent extends WorkspaceEvent {

	private final Long workspaceMemberId;
	private final String nickname;
	private final WorkspaceRole workspaceRole;

	public MemberJoinedWorkspaceEvent(
		String workspaceCode,
		Long triggeredByWorkspaceMemberId,
		Long workspaceMemberId,
		String nickname,
		WorkspaceRole workspaceRole
	) {
		super(
			NotificationType.MEMBER_JOINED_WORKSPACE,
			ResourceType.WORKSPACE,
			workspaceCode,
			triggeredByWorkspaceMemberId
		);

		this.workspaceMemberId = workspaceMemberId;
		this.nickname = nickname;
		this.workspaceRole = workspaceRole;
	}

	public static MemberJoinedWorkspaceEvent createEvent(
		WorkspaceMember workspaceMember
	) {
		return new MemberJoinedWorkspaceEvent(
			workspaceMember.getWorkspaceCode(),
			workspaceMember.getId(),
			workspaceMember.getId(),
			workspaceMember.getNickname(),
			workspaceMember.getRole()
		);
	}

	@Override
	public EntityReference createEntityReference() {
		return EntityReference.forWorkspaceMember(getWorkspaceCode(), getWorkspaceMemberId());
	}
}
