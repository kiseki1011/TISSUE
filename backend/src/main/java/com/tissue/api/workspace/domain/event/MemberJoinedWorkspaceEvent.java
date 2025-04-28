package com.tissue.api.workspace.domain.event;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.vo.EntityReference;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import lombok.Getter;

@Getter
public class MemberJoinedWorkspaceEvent extends WorkspaceEvent {

	private final String nickname;
	private final WorkspaceRole workspaceRole;

	public MemberJoinedWorkspaceEvent(
		String workspaceCode,
		Long actorMemberId,
		String nickname,
		WorkspaceRole workspaceRole
	) {
		super(
			NotificationType.MEMBER_JOINED_WORKSPACE,
			ResourceType.WORKSPACE,
			workspaceCode,
			actorMemberId
		);

		this.nickname = nickname;
		this.workspaceRole = workspaceRole;
	}

	public static MemberJoinedWorkspaceEvent createEvent(
		WorkspaceMember workspaceMember
	) {
		return new MemberJoinedWorkspaceEvent(
			workspaceMember.getWorkspaceCode(),
			workspaceMember.getId(),
			workspaceMember.getNickname(),
			workspaceMember.getRole()
		);
	}

	@Override
	public EntityReference createEntityReference() {
		return EntityReference.forWorkspaceMember(getWorkspaceCode(), getActorMemberId());
	}
}
