package com.uranus.taskmanager.fixture;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.invitation.InvitationStatus;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;

public class TestFixture {

	Workspace workspace = Workspace.builder()
		.code("testcode")
		.name("test name")
		.description("test description")
		.build();

	Member member = Member.builder()
		.loginId("user123")
		.email("user123@test.com")
		.password("test1234!")
		.build();

	Invitation pendingInvitation = Invitation.builder()
		.status(InvitationStatus.PENDING)
		.workspace(workspace)
		.member(member)
		.build();

	Invitation acceptedInvitation = Invitation.builder()
		.status(InvitationStatus.ACCEPTED)
		.workspace(workspace)
		.member(member)
		.build();

	WorkspaceMember adminWorkspaceMember = WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.ADMIN,
		member.getEmail());

	WorkspaceMember userWorkspaceMember = WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.USER,
		member.getEmail());

	public Workspace createWorkspace(String code) {
		return Workspace.builder()
			.code(code)
			.name("test name")
			.description("test description")
			.password("workspace1234!")
			.build();
	}

	public Workspace createWorkspaceWithPassword(String code, String password) {
		return Workspace.builder()
			.code(code)
			.name("test name")
			.description("test description")
			.password(password)
			.build();
	}

	public Workspace createWorkspaceWithoutPassword(String code) {
		return Workspace.builder()
			.code(code)
			.name("test name")
			.description("test description")
			.build();
	}

	public Member createMember(String loginId, String email) {
		return Member.builder()
			.loginId(loginId)
			.email(email)
			.password("test1234!")
			.build();
	}

	public LoginMemberDto createLoginMemberDto(String loginId, String email) {
		return LoginMemberDto.builder()
			.loginId(loginId)
			.email(email)
			.build();
	}

	public WorkspaceMember createAdminWorkspaceMember(Member member, Workspace workspace) {
		return WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.ADMIN,
			member.getEmail());
	}

	public WorkspaceMember createUserWorkspaceMember(Member member, Workspace workspace) {
		return WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.USER,
			member.getEmail());
	}

	public LoginMemberDto createLoginMember(String loginId, String email) {
		return LoginMemberDto.builder()
			.loginId(loginId)
			.email(email)
			.build();
	}

	public Invitation createPendingInvitation(Workspace workspace, Member member) {
		return Invitation.builder()
			.status(InvitationStatus.PENDING)
			.workspace(workspace)
			.member(member)
			.build();
	}

	public Invitation createAcceptedInvitation(Workspace workspace, Member member) {
		return Invitation.builder()
			.status(InvitationStatus.ACCEPTED)
			.workspace(workspace)
			.member(member)
			.build();
	}

}
