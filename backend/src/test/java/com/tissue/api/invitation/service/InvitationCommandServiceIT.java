package com.tissue.api.invitation.service;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.invitation.domain.Invitation;
import com.tissue.api.invitation.domain.InvitationStatus;
import com.tissue.api.invitation.presentation.dto.response.AcceptInvitationResponse;
import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.presentation.dto.request.InviteMembersRequest;
import com.tissue.helper.ServiceIntegrationTestHelper;

class InvitationCommandServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("초대 수락에 성공하면 초대 수락 응답을 반환 받는다")
	void testAcceptInvitation_ifSuccess_returnsAcceptInvitationResponse() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TESTCODE",
			null);
		workspaceRepository.save(workspace);

		Member invitedMember = memberRepositoryFixture.createAndSaveMember(
			"invitedMember",
			"invitedmember@test.com",
			"password1234!"
		);
		memberRepository.save(invitedMember);

		workspaceMemberInviteService.inviteMembers("TESTCODE", InviteMembersRequest.of(Set.of("invitedMember")));

		// when
		AcceptInvitationResponse response = invitationCommandService.acceptInvitation(invitedMember.getId(), 1L);

		// then
		assertThat(response).isNotNull();
		assertThat(response.workspaceCode()).isEqualTo("TESTCODE");
		assertThat(response.invitationId()).isEqualTo(1L);
	}

	@Test
	@DisplayName("초대가 성공하면 초대의 상태가 ACCEPTED로 변경된다")
	void testAcceptInvitation_ifSuccess_invitationStatusChangeToAccepted() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TESTCODE",
			null
		);
		workspaceRepository.save(workspace);

		Member invitedMember = memberRepositoryFixture.createAndSaveMember(
			"invitedMember",
			"invitedmember@test.com",
			"password1234!"
		);
		memberRepository.save(invitedMember);

		workspaceMemberInviteService.inviteMembers("TESTCODE", InviteMembersRequest.of(Set.of("invitedMember")));

		// when
		invitationCommandService.acceptInvitation(invitedMember.getId(), 1L);

		// then
		Invitation invitation = invitationRepository.findById(workspace.getId()).get();
		assertThat(invitation).isNotNull();
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
	}

	@Test
	@DisplayName("초대의 수락이 성공하면 해당 워크스페이스에 참여 된다")
	void testAcceptInvitation_ifSuccess_memberJoinsWorkspace() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TESTCODE",
			null
		);
		workspaceRepository.save(workspace);

		Member invitedMember = memberRepositoryFixture.createAndSaveMember(
			"invitedMember",
			"invitedmember@test.com",
			"password1234!"
		);
		memberRepository.save(invitedMember);

		workspaceMemberInviteService.inviteMembers("TESTCODE", InviteMembersRequest.of(Set.of("invitedMember")));

		// when
		invitationCommandService.acceptInvitation(invitedMember.getId(), 1L);

		// then
		WorkspaceMember workspaceMember = workspaceMemberRepository.findByMemberIdAndWorkspaceId(invitedMember.getId(),
			workspace.getId()).get();
		assertThat(workspaceMember).isNotNull();
		assertThat(workspaceMember.getNickname()).isEqualTo("invitedmember@test.com");
	}

	@Test
	@DisplayName("유효하지 않은 초대 코드를 사용해서 초대를 수락하면 예외가 발생한다")
	void testAcceptInvitation_ifUseInvalidCode_throwsException() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TESTCODE",
			null
		);
		workspaceRepository.save(workspace);

		Member invitedMember = memberRepositoryFixture.createAndSaveMember(
			"invitedMember",
			"invitedmember@test.com",
			"password1234!"
		);
		memberRepository.save(invitedMember);

		workspaceMemberInviteService.inviteMembers("TESTCODE", InviteMembersRequest.of(Set.of("invitedMember")));

		// when & then
		assertThatThrownBy(() -> invitationCommandService.acceptInvitation(invitedMember.getId(), 2L))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("초대 수락 시, 초대의 상태가 PENDING이 아니면 예외가 발생한다")
	void testAcceptInvitation_whenInvitationStatusNotPending_throwsException() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TESTCODE",
			null
		);
		workspaceRepository.save(workspace);

		Member invitedMember = memberRepositoryFixture.createAndSaveMember(
			"invitedMember",
			"invitedmember@test.com",
			"password1234!"
		);
		memberRepository.save(invitedMember);

		workspaceMemberInviteService.inviteMembers("TESTCODE", InviteMembersRequest.of(Set.of("invitedMember")));

		// 초대를 거절해서 초대 상태를 REJECTED로 변경
		invitationCommandService.rejectInvitation(invitedMember.getId(), 1L);

		// when & then
		assertThatThrownBy(() -> invitationCommandService.acceptInvitation(invitedMember.getId(), 2L))
			.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@DisplayName("초대를 거절하면 초대 상태가 REJECTED로 변경된다")
	void testRejectInvitation_ifSuccess_invitationStatusChangeToRejected() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TESTCODE",
			null
		);
		workspaceRepository.save(workspace);

		Member invitedMember = memberRepositoryFixture.createAndSaveMember(
			"invitedMember",
			"invitedmember@test.com",
			"password1234!"
		);
		memberRepository.save(invitedMember);

		workspaceMemberInviteService.inviteMembers("TESTCODE", InviteMembersRequest.of(Set.of("invitedMember")));

		// when
		invitationCommandService.rejectInvitation(invitedMember.getId(), 1L);

		// then
		Invitation invitation = invitationRepository.findById(workspace.getId()).get();
		assertThat(invitation).isNotNull();
		assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
	}

	@Test
	@DisplayName("사용자의 ACCEPTED, REJECTED 상태인 초대를 모두 삭제한다")
	@Transactional
	void deleteInvitations() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"tester",
			"test@test.com",
			"test1234!"
		);

		Workspace workspace1 = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Workspace1",
			"Description1",
			"TESTCODE1",
			null
		);

		Workspace workspace2 = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Workspace2",
			"Description2",
			"TESTCODE2",
			null
		);

		Workspace workspace3 = workspaceRepositoryFixture.createAndSaveWorkspace(
			"Workspace3",
			"Description3",
			"TESTCODE3",
			null
		);

		invitationRepositoryFixture.createAndSaveInvitation(
			workspace1,
			member,
			InvitationStatus.ACCEPTED
		);

		invitationRepositoryFixture.createAndSaveInvitation(
			workspace2,
			member,
			InvitationStatus.REJECTED
		);

		Invitation pendingInvitation = invitationRepositoryFixture.createAndSaveInvitation(
			workspace3,
			member,
			InvitationStatus.PENDING
		);

		entityManager.flush();
		entityManager.clear();

		// when
		invitationCommandService.deleteInvitations(member.getId());

		// then
		List<Invitation> remainingInvitations = invitationRepository.findAllByMemberId(member.getId());
		assertThat(remainingInvitations.get(0).getId()).isEqualTo(pendingInvitation.getId());
	}
}
