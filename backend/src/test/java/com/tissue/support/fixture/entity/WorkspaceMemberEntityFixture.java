package com.tissue.support.fixture.entity;

import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.enums.WorkspaceRole;

public class WorkspaceMemberEntityFixture {
	public WorkspaceMember createOwnerWorkspaceMember(Member member, Workspace workspace) {
		return WorkspaceMember.createWorkspaceMember(member, workspace, WorkspaceRole.OWNER);
	}

	public WorkspaceMember createManagerWorkspaceMember(Member member, Workspace workspace) {
		return WorkspaceMember.createWorkspaceMember(member, workspace, WorkspaceRole.MANAGER);
	}

	public WorkspaceMember createMemberWorkspaceMember(Member member, Workspace workspace) {
		return WorkspaceMember.createWorkspaceMember(member, workspace, WorkspaceRole.MEMBER);
	}
}
