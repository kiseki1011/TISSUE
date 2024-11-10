package com.uranus.taskmanager.fixture.entity;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

public class WorkspaceMemberEntityFixture {
	public WorkspaceMember createAdminWorkspaceMember(Member member, Workspace workspace) {
		return WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.MANAGER,
			member.getEmail());
	}

	public WorkspaceMember createUserWorkspaceMember(Member member, Workspace workspace) {
		return WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.COLLABORATOR,
			member.getEmail());
	}
}
