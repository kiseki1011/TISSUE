package com.tissue.fixture.entity;

import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

public class WorkspaceMemberEntityFixture {
	public WorkspaceMember createOwnerWorkspaceMember(Member member, Workspace workspace) {
		return WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.OWNER,
			member.getEmail());
	}

	public WorkspaceMember createManagerWorkspaceMember(Member member, Workspace workspace) {
		return WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.MANAGER,
			member.getEmail());
	}

	public WorkspaceMember createCollaboratorWorkspaceMember(Member member, Workspace workspace) {
		return WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.COLLABORATOR,
			member.getEmail());
	}
}
