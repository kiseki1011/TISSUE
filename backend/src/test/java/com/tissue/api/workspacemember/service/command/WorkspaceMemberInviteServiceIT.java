package com.tissue.api.workspacemember.service.command;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.common.exception.InvalidOperationException;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.InvitationStatus;
import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.tissue.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

class WorkspaceMemberInviteServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("초대가 성공하면 초대가 PENDING 상태로 저장된다")
	void testInviteMembers_ifSuccess_invitationStatusIsPending() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		InviteMembersRequest request = InviteMembersRequest.of(Set.of("member1"));

		// when
		workspaceMemberInviteService.inviteMembers("TESTCODE", request);

		// then
		Invitation invitation = invitationRepository.findByWorkspaceCodeAndMemberId("TESTCODE", member.getId()).get();
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	@DisplayName("존재하지 않는 멤버에 대한 초대는 초대 대상에서 제외된다")
	void testInviteMembers_ifMemberNotExist_excludedFromInvite() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		InviteMembersRequest request = InviteMembersRequest.of(
			Set.of("nonExistentMember1", "nonExistentMember2", "member1")
		);

		// when
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers("TESTCODE", request);

		// then
		assertThat(response.invitedMembers().get(0)).isEqualTo(InviteMembersResponse.InvitedMember.from(member));
	}

	@Test
	@DisplayName("다수의 멤버를 초대할 때 존재하지 않는 멤버는 대상에서 제외되고, 존재하는 멤버는 초대된다")
	void testInviteMembers_memberNotExistExcluded_memberExistInvited() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member member2 = memberRepositoryFixture.createAndSaveMember(
			"member2",
			"member2@test.com",
			"password1234!"
		);

		Member member3 = memberRepositoryFixture.createAndSaveMember(
			"member3",
			"member3@test.com",
			"password1234!"
		);

		InviteMembersRequest request = InviteMembersRequest.of(Set.of("nonExistingMember", "member2", "member3"));

		// when
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers("TESTCODE", request);

		// then
		assertThat(response.invitedMembers()).contains(
			InviteMembersResponse.InvitedMember.from(member2),
			InviteMembersResponse.InvitedMember.from(member3)
		);
	}

	@Test
	@DisplayName("해당 워크스페이스에 이미 참여하고 있는 멤버는 초대 대상에서 제외된다")
	void testInviteMembers_AlreadyJoinedMemberExcluded() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member member2 = memberRepositoryFixture.createAndSaveMember(
			"member2",
			"member2@test.com",
			"password1234!"
		);

		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);
		workspaceRepositoryFixture.addAndSaveMemberToWorkspace(member, workspace, WorkspaceRole.MEMBER);

		InviteMembersRequest request = InviteMembersRequest.of(
			Set.of("member2", "member1")
		);

		// when
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers("TESTCODE", request);

		// then
		assertThat(response.invitedMembers().get(0)).isEqualTo(InviteMembersResponse.InvitedMember.from(member2));
		assertThat(response.workspaceCode()).isEqualTo("TESTCODE");
	}

	@Test
	@DisplayName("멤버 식별자들을 필터링 후 초대 대상에 대한 리스트가 비어 있으면 예외가 발생한다")
	void testInviteMembers_ifListOfInvitedMembersEmpty_throwException() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test Workspace",
			"Test Description",
			"TESTCODE",
			null
		);

		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);
		workspaceRepositoryFixture.addAndSaveMemberToWorkspace(member, workspace, WorkspaceRole.MEMBER);

		InviteMembersRequest request = InviteMembersRequest.of(Set.of("member1"));

		// when & then
		assertThatThrownBy(() -> workspaceMemberInviteService.inviteMembers("TESTCODE", request))
			.isInstanceOf(InvalidOperationException.class);
	}
}
