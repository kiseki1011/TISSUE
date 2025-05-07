package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.InvitationStatus;
import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.tissue.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

class WorkspaceMemberInviteServiceIT extends ServiceIntegrationTestHelper {

	Workspace workspace;

	@BeforeEach
	void setUp() {
		// create workspace
		workspace = testDataFixture.createWorkspace("test workspace", null, null);
	}

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("초대가 성공하면 초대가 PENDING 상태로 저장된다")
	void canInviteMemberToWorkspace_InvitationStatusIsPending() {
		// given
		Member member = testDataFixture.createMember("member1");

		// when
		workspaceMemberInviteService.inviteMembers(workspace.getCode(), new InviteMembersRequest(Set.of("member1")));

		// then
		Invitation invitation = invitationRepository.findByWorkspaceCodeAndMemberId(workspace.getCode(), member.getId())
			.get();

		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	@DisplayName("다수의 멤버를 워크스페이스로 초대할 수 있다")
	void canInviteMultipleMembersAtOnceToWorkspace() {
		// given
		Member member1 = testDataFixture.createMember("member1");
		Member member2 = testDataFixture.createMember("member2");

		InviteMembersRequest request = new InviteMembersRequest(Set.of("member1", "member2"));

		// when
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers(workspace.getCode(), request);

		// then
		assertThat(response.invitedMembers()).contains(
			InviteMembersResponse.InvitedMember.from(member1),
			InviteMembersResponse.InvitedMember.from(member2)
		);
	}

	@Test
	@DisplayName("다수의 멤버에게 초대를 보낼때, 존재하지 않는 멤버에 대한 초대는 대상에서 제외된다")
	void whenInvitingMultipleMembers_NonExistingIdentifiersAreExcluded() {
		// given
		Member member = testDataFixture.createMember("member1");

		// "invalid1", "invalid2" are non existing member identifiers
		InviteMembersRequest request = new InviteMembersRequest(
			Set.of("invalid1", "invalid2", "member1")
		);

		// when
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers(workspace.getCode(), request);

		// then
		assertThat(response.invitedMembers().get(0)).isEqualTo(InviteMembersResponse.InvitedMember.from(member));
	}

	@Test
	@DisplayName("해당 워크스페이스에 이미 참여하고 있는 멤버는 초대 대상에서 제외된다")
	void whenInvitingMembers_MembersThatAlreadyJoinedAreExcluded() {
		// given
		Member member1 = testDataFixture.createMember("member1");
		Member member2 = testDataFixture.createMember("member2");

		// assume that member1 joined the workspace
		WorkspaceMember workspaceMember1 = testDataFixture.createWorkspaceMember(
			member1,
			workspace,
			WorkspaceRole.MEMBER
		);

		InviteMembersRequest request = new InviteMembersRequest(
			Set.of("member1", "member2")
		);

		// when
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers(workspace.getCode(), request);

		// then
		assertThat(response.invitedMembers().get(0)).isEqualTo(InviteMembersResponse.InvitedMember.from(member2));
		assertThat(response.workspaceCode()).isEqualTo(workspace.getCode());
	}

	@Test
	@DisplayName("멤버를 초대할 때, 하나 이상의 유효한 초대가 존재해야 한다")
	void whenInvitingMembers_AtLeast_A_SingleInvitationMustBeValid() {
		// given
		Member member = testDataFixture.createMember("member1");
		WorkspaceMember workspaceMember = testDataFixture.createWorkspaceMember(
			member,
			workspace,
			WorkspaceRole.MEMBER
		);

		// invitation list will be empty since member1 is already a workspace member
		InviteMembersRequest request = new InviteMembersRequest(Set.of("member1"));

		// when & then
		assertThatThrownBy(() -> workspaceMemberInviteService.inviteMembers(workspace.getCode(), request))
			.isInstanceOf(InvalidOperationException.class);
	}
}
