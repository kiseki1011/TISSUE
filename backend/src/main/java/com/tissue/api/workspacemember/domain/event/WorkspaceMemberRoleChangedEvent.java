package com.tissue.api.workspacemember.domain.event;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.vo.EntityReference;
import com.tissue.api.workspace.domain.event.WorkspaceEvent;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import lombok.Getter;

@Getter
public class WorkspaceMemberRoleChangedEvent extends WorkspaceEvent {

	private final Long targetWorkspaceMemberId;
	private final String targetNickname;
	private final WorkspaceRole oldRole;
	private final WorkspaceRole newRole;

	public WorkspaceMemberRoleChangedEvent(
		String workspaceCode,
		Long triggeredByWorkspaceMemberId,
		Long targetWorkspaceMemberId,
		String targetNickname,
		WorkspaceRole oldRole,
		WorkspaceRole newRole
	) {
		super(
			NotificationType.WORKSPACE_MEMBER_ROLE_CHANGED,
			ResourceType.WORKSPACE,
			workspaceCode,
			triggeredByWorkspaceMemberId
		);

		this.targetWorkspaceMemberId = targetWorkspaceMemberId;
		this.targetNickname = targetNickname;
		this.oldRole = oldRole;
		this.newRole = newRole;
	}

	public static WorkspaceMemberRoleChangedEvent createEvent(
		WorkspaceMember workspaceMember,
		WorkspaceRole oldRole,
		Long triggeredByWorkspaceMemberId
	) {
		return new WorkspaceMemberRoleChangedEvent(
			workspaceMember.getWorkspaceCode(),
			triggeredByWorkspaceMemberId,
			workspaceMember.getMember().getId(),
			workspaceMember.getNickname(),
			oldRole,
			workspaceMember.getRole()
		);
	}

	@Override
	public EntityReference createEntityReference() {
		return EntityReference.forWorkspaceMember(getWorkspaceCode(), getTargetWorkspaceMemberId());
	}
}
