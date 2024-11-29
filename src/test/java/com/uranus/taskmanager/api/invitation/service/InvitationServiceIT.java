package com.uranus.taskmanager.api.invitation.service;

import org.junit.jupiter.api.AfterEach;

import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class InvitationServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	// @Test
	// @DisplayName("초대 수락에 성공하면 초대 수락 응답을 반환 받는다")
	// void test1() {
	// 	// given
	// 	Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TESTCODE",
	// 		null);
	// 	workspaceRepository.save(workspace);
	//
	// 	Member invitedMember = memberRepositoryFixture.createMember("invitedMember", "invitedmember@test.com",
	// 		"password1234!");
	// 	memberRepository.save(invitedMember);
	//
	// 	workspaceMemberInviteService.inviteMember("TESTCODE", new InviteMemberRequest("invitedMember"));
	//
	// 	// when
	// 	AcceptInvitationResponse response = invitationService.acceptInvitation(invitedMember.getId(), "TESTCODE");
	//
	// 	// then
	// 	assertThat(response).isNotNull();
	// 	assertThat(response.getWorkspaceCode()).isEqualTo("TESTCODE");
	// 	assertThat(response.getInvitationId()).isEqualTo(1L);
	// }

	// @Test
	// @DisplayName("초대가 성공하면 초대의 상태가 ACCEPTED로 변경된다")
	// void test2() {
	// 	// given
	// 	Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TESTCODE",
	// 		null);
	// 	workspaceRepository.save(workspace);
	//
	// 	Member invitedMember = memberRepositoryFixture.createMember("invitedMember", "invitedmember@test.com",
	// 		"password1234!");
	// 	memberRepository.save(invitedMember);
	//
	// 	workspaceMemberInviteService.inviteMember("TESTCODE", new InviteMemberRequest("invitedMember"));
	//
	// 	// when
	// 	invitationService.acceptInvitation(invitedMember.getId(), "TESTCODE");
	//
	// 	// then
	// 	Invitation invitation = invitationRepository.findById(workspace.getId()).get();
	// 	assertThat(invitation).isNotNull();
	// 	assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
	// }

	// @Test
	// @DisplayName("초대의 수락이 성공하면 해당 워크스페이스에 참여 된다")
	// void test3() {
	// 	// given
	// 	Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TESTCODE",
	// 		null);
	// 	workspaceRepository.save(workspace);
	//
	// 	Member invitedMember = memberRepositoryFixture.createMember("invitedMember", "invitedmember@test.com",
	// 		"password1234!");
	// 	memberRepository.save(invitedMember);
	//
	// 	workspaceMemberInviteService.inviteMember("TESTCODE", new InviteMemberRequest("invitedMember"));
	//
	// 	// when
	// 	invitationService.acceptInvitation(invitedMember.getId(), "TESTCODE");
	//
	// 	// then
	// 	WorkspaceMember workspaceMember = workspaceMemberRepository.findByMemberIdAndWorkspaceId(invitedMember.getId(),
	// 		workspace.getId()).get();
	// 	assertThat(workspaceMember).isNotNull();
	// 	assertThat(workspaceMember.getNickname()).isEqualTo("invitedmember@test.com");
	// }

	// @Test
	// @DisplayName("유효하지 않은 초대 코드를 사용해서 초대를 수락하면 예외가 발생한다")
	// void test4() {
	// 	// given
	// 	Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TESTCODE",
	// 		null);
	// 	workspaceRepository.save(workspace);
	//
	// 	Member invitedMember = memberRepositoryFixture.createMember("invitedMember", "invitedmember@test.com",
	// 		"password1234!");
	// 	memberRepository.save(invitedMember);
	//
	// 	workspaceMemberInviteService.inviteMember("TESTCODE", new InviteMemberRequest("invitedMember"));
	//
	// 	// when & then
	// 	assertThatThrownBy(() -> invitationService.acceptInvitation(invitedMember.getId(), "INVALIDCODE")).isInstanceOf(
	// 		InvitationNotFoundException.class);
	// }

	// @Test
	// @DisplayName("초대 수락 시, 초대의 상태가 PENDING이 아니면 예외가 발생한다")
	// void test5() {
	// 	// given
	// 	Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TESTCODE",
	// 		null);
	// 	workspaceRepository.save(workspace);
	//
	// 	Member invitedMember = memberRepositoryFixture.createMember("invitedMember", "invitedmember@test.com",
	// 		"password1234!");
	// 	memberRepository.save(invitedMember);
	//
	// 	workspaceMemberInviteService.inviteMember("TESTCODE", new InviteMemberRequest("invitedMember"));
	//
	// 	// 초대를 거절해서 초대 상태를 REJECTED로 변경
	// 	invitationService.rejectInvitation(invitedMember.getId(), "TESTCODE");
	//
	// 	// when & then
	// 	assertThatThrownBy(() -> invitationService.acceptInvitation(invitedMember.getId(), "TESTCODE")).isInstanceOf(
	// 		InvitationNotFoundException.class);
	// }

	// @Test
	// @DisplayName("초대를 거절하면 초대 상태가 REJECTED로 변경된다")
	// void test6() {
	// 	// given
	// 	Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TESTCODE",
	// 		null);
	// 	workspaceRepository.save(workspace);
	//
	// 	Member invitedMember = memberRepositoryFixture.createMember("invitedMember", "invitedmember@test.com",
	// 		"password1234!");
	// 	memberRepository.save(invitedMember);
	//
	// 	workspaceMemberInviteService.inviteMember("TESTCODE", new InviteMemberRequest("invitedMember"));
	//
	// 	// when
	// 	invitationService.rejectInvitation(invitedMember.getId(), "TESTCODE");
	//
	// 	// then
	// 	Invitation invitation = invitationRepository.findById(workspace.getId()).get();
	// 	assertThat(invitation).isNotNull();
	// 	assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
	// }
}
