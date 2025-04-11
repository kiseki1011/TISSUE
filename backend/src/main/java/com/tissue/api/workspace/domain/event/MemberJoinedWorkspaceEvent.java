package com.tissue.api.workspace.domain.event;

import com.tissue.api.notification.domain.enums.NotificationType;
import com.tissue.api.notification.domain.enums.ResourceType;
import com.tissue.api.notification.domain.vo.EntityReference;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import lombok.Getter;

@Getter
public class MemberJoinedWorkspaceEvent extends WorkspaceEvent {

	private final Long memberId;
	private final Long workspaceMemberId;
	private final String loginId;
	private final String nickname;
	private final WorkspaceRole workspaceRole;

	public MemberJoinedWorkspaceEvent(
		String workspaceCode,
		Long triggeredByWorkspaceMemberId,
		Long memberId,
		Long workspaceMemberId,
		String loginId,
		String nickname,
		WorkspaceRole workspaceRole
	) {
		super(
			NotificationType.MEMBER_JOINED_WORKSPACE,
			ResourceType.WORKSPACE,
			workspaceCode,
			triggeredByWorkspaceMemberId
		);

		this.memberId = memberId;
		this.workspaceMemberId = workspaceMemberId;
		this.loginId = loginId;
		this.nickname = nickname;
		this.workspaceRole = workspaceRole;
	}

	public static MemberJoinedWorkspaceEvent createEvent(
		WorkspaceMember workspaceMember,
		Long triggeredByWorkspaceMemberId
	) {
		return new MemberJoinedWorkspaceEvent(
			workspaceMember.getWorkspaceCode(),
			triggeredByWorkspaceMemberId,
			workspaceMember.getMember().getId(),
			workspaceMember.getId(),
			workspaceMember.getMember().getLoginId(),
			workspaceMember.getNickname(),
			workspaceMember.getRole()
		);
	}

	@Override
	public EntityReference createEntityReference() {
		return EntityReference.forWorkspaceMember(getWorkspaceCode(), getWorkspaceMemberId());
	}
}
