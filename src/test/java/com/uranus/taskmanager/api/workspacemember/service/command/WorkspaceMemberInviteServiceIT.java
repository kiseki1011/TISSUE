package com.uranus.taskmanager.api.workspacemember.service.command;

import static org.assertj.core.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class WorkspaceMemberInviteServiceIT extends ServiceIntegrationTestHelper {

	private Member member;

	@BeforeEach
	void setUp() {
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("Test Workspace", "Test Description",
			"TESTCODE", null);
		member = memberRepositoryFixture.createMember("member1", "member1@test.com", "password1234!");
		workspaceRepositoryFixture.addMemberToWorkspace(member, workspace, WorkspaceRole.COLLABORATOR);
	}

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("초대가 성공하면 초대가 PENDING 상태로 저장된다")
	void testInviteMember_ifSuccess_invitationStatusIsPending() {
		// given
		Member member2 = memberRepositoryFixture.createMember("member2", "member2@test.com", "password1234!");
		InviteMembersRequest request = InviteMembersRequest.of(Set.of("member2"));

		// when
		workspaceMemberInviteService.inviteMembers("TESTCODE", request);

		// then
		Invitation invitation = invitationRepository.findByWorkspaceCodeAndMemberId("TESTCODE", member2.getId()).get();
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.PENDING);
	}

	@Test
	@DisplayName("존재하지 않는 멤버에 대한 단일 초대는 초대 대상에서 제외된다")
	void testInviteMember_ifMemberNotExist_excludedFromInvite() {
		// given
		InviteMembersRequest request = InviteMembersRequest.of(Set.of("nonExistentMember"));

		// when
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers("TESTCODE", request);

		// then
		assertThat(response.getTotalInvitedMembers()).isZero();
	}

	@Test
	@DisplayName("다수의 멤버를 초대할 때 존재하지 않는 멤버는 대상에서 제외되고, 존재하는 멤버는 초대된다")
	void testInviteMembers_memberNotExistExcluded_memberExistInvited() {
		// given
		Member member2 = memberRepositoryFixture.createMember("member2", "member2@test.com", "password1234!");
		Member member3 = memberRepositoryFixture.createMember("member3", "member3@test.com", "password1234!");
		InviteMembersRequest request = InviteMembersRequest.of(Set.of("nonExistingMember", "member2", "member3"));

		// when
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers("TESTCODE", request);

		// then
		assertThat(response.getTotalInvitedMembers()).isEqualTo(2L);

		assertThat(response.getInvitedMembers()).contains(
			InviteMembersResponse.InvitedMember.from(member2),
			InviteMembersResponse.InvitedMember.from(member3)
		);
	}

	@Test
	@DisplayName("해당 워크스페이스에 이미 참여하고 있는 멤버를 단일 초대하면 초대 대상에서 제외되고, 응답에서도 제외된다")
	void testInviteMember_AlreadyJoinedWorkspaceException() {
		// given
		InviteMembersRequest request = InviteMembersRequest.of(Set.of("member1"));

		// when
		InviteMembersResponse response = workspaceMemberInviteService.inviteMembers("TESTCODE", request);

		// then
		assertThat(response.getInvitedMembers()).isEmpty();
		assertThat(response.getWorkspaceCode()).isEqualTo("TESTCODE");
	}
}
