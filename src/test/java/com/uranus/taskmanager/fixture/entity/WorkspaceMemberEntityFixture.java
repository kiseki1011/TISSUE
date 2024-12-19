package com.uranus.taskmanager.fixture.entity;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;

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
