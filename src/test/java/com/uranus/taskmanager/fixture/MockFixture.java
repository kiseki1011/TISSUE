package com.uranus.taskmanager.fixture;

import com.uranus.taskmanager.api.auth.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

public class MockFixture {

	public Workspace mockWorkspace(String workspaceCode) {
		return Workspace.builder()
			.workspaceCode(workspaceCode)
			.name("test name")
			.description("test description")
			.build();
	}

	public Member mockMember(String loginId, String email) {
		return Member.builder()
			.loginId(loginId)
			.email(email)
			.password("test1234!")
			.build();
	}

	public WorkspaceMember mockAdminWorkspaceMember(Member mockMember, Workspace mockWorkspace) {
		return WorkspaceMember.addWorkspaceMember(mockMember, mockWorkspace, WorkspaceRole.ADMIN,
			mockMember.getEmail());
	}

	public LoginMemberDto mockLoginMember(String loginId, String email) {
		return LoginMemberDto.builder()
			.loginId(loginId)
			.email(email)
			.build();
	}
}
