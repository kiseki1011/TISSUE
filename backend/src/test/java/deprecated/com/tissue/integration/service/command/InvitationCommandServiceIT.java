package deprecated.com.tissue.integration.service.command;

import deprecated.com.tissue.support.helper.ServiceIntegrationTestHelper;

class InvitationCommandServiceIT extends ServiceIntegrationTestHelper {

	// @AfterEach
	// public void tearDown() {
	// 	databaseCleaner.execute();
	// }
	//
	// @Test
	// @DisplayName("나에게 온 초대가 존재한다면 해당 초대를 수락할 수 있다")
	// void canAcceptInvitation() {
	// 	// given
	// 	Long invitationId = 1L;
	// 	Workspace workspace = testDataFixture.createWorkspace("test workspace", null, null);
	// 	Member member = testDataFixture.createMember("member1");
	//
	// 	// send invitation
	// 	workspaceMemberInviteService.inviteMembers(workspace.getKey(), InviteMembersRequest.of(Set.of("member1")));
	//
	// 	// when
	// 	InvitationResponse response = invitationCommandService.acceptInvitation(member.getId(), 1L);
	//
	// 	// then
	// 	assertThat(response).isNotNull();
	// 	assertThat(response.workspaceCode()).isEqualTo(workspace.getKey());
	// 	assertThat(response.invitationId()).isEqualTo(invitationId);
	// }
	//
	// @Test
	// @DisplayName("초대가 성공하면 초대의 상태가 ACCEPTED로 변경된다")
	// void acceptInvitation_InvitationStatusChangeToAccepted() {
	// 	// given
	// 	Long invitationId = 1L;
	// 	Workspace workspace = testDataFixture.createWorkspace("test workspace", null, null);
	// 	Member member = testDataFixture.createMember("member1");
	//
	// 	workspaceMemberInviteService.inviteMembers(workspace.getKey(), InviteMembersRequest.of(Set.of("member1")));
	//
	// 	// when
	// 	invitationCommandService.acceptInvitation(member.getId(), invitationId);
	//
	// 	// then
	// 	Invitation invitation = invitationRepository.findById(invitationId).get();
	// 	assertThat(invitation).isNotNull();
	// 	assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
	// }
	//
	// @Test
	// @DisplayName("초대의 수락이 성공하면 해당 워크스페이스에 참여 된다")
	// void acceptInvitation_AcceptorJoinsWorkspace() {
	// 	// given
	// 	Long invitationId = 1L;
	// 	Workspace workspace = testDataFixture.createWorkspace("test workspace", null, null);
	// 	Member member = testDataFixture.createMember("member1");
	//
	// 	workspaceMemberInviteService.inviteMembers(workspace.getKey(), InviteMembersRequest.of(Set.of("member1")));
	//
	// 	// when
	// 	invitationCommandService.acceptInvitation(member.getId(), invitationId);
	//
	// 	// then
	// 	WorkspaceMember workspaceMember = workspaceMemberRepository.findByMemberIdAndWorkspaceId(member.getId(),
	// 		workspace.getId()).get();
	//
	// 	assertThat(workspaceMember).isNotNull();
	// 	assertThat(workspaceMember.getMember().getId()).isEqualTo(member.getId());
	// }
	//
	// @Test
	// @DisplayName("유효하지 않은 id를 통해 초대를 수락할 수 없다")
	// void cannotAcceptInvitationThroughInvalidInvitationId() {
	// 	// given
	// 	Long invalidInvitationId = 999L;
	// 	Workspace workspace = testDataFixture.createWorkspace("test workspace", null, null);
	// 	Member member = testDataFixture.createMember("member1");
	//
	// 	workspaceMemberInviteService.inviteMembers(workspace.getKey(), InviteMembersRequest.of(Set.of("member1")));
	//
	// 	// when & then
	// 	assertThatThrownBy(() -> invitationCommandService.acceptInvitation(member.getId(), invalidInvitationId))
	// 		.isInstanceOf(ResourceNotFoundException.class);
	// }
	//
	// @Test
	// @DisplayName("초대의 상태가 PENDING이 아닌 초대는 수락할 수 없다")
	// void cannotAcceptInvitationIfInvitationStatusIsNotPending() {
	// 	// given
	// 	Long invitationId = 1L;
	// 	Workspace workspace = testDataFixture.createWorkspace("test workspace", null, null);
	// 	Member member = testDataFixture.createMember("member1");
	//
	// 	workspaceMemberInviteService.inviteMembers(workspace.getKey(), InviteMembersRequest.of(Set.of("member1")));
	//
	// 	// reject invitation - invitation status is changed to REJECTED
	// 	invitationCommandService.rejectInvitation(member.getId(), invitationId);
	//
	// 	// when & then
	// 	assertThatThrownBy(() -> invitationCommandService.acceptInvitation(member.getId(), invitationId))
	// 		.isInstanceOf(ResourceNotFoundException.class);
	// }
	//
	// @Test
	// @DisplayName("초대를 거절하면 초대 상태가 REJECTED로 변경된다")
	// void rejectInvitation_InvitationStatusChangeToRejected() {
	// 	// given
	// 	Long invitationId = 1L;
	// 	Workspace workspace = testDataFixture.createWorkspace("test workspace", null, null);
	// 	Member member = testDataFixture.createMember("member1");
	//
	// 	workspaceMemberInviteService.inviteMembers(workspace.getKey(), InviteMembersRequest.of(Set.of("member1")));
	//
	// 	// when
	// 	invitationCommandService.rejectInvitation(member.getId(), invitationId);
	//
	// 	// then
	// 	Invitation invitation = invitationRepository.findById(workspace.getId()).get();
	//
	// 	assertThat(invitation).isNotNull();
	// 	assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.REJECTED);
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("사용자의 ACCEPTED, REJECTED 상태인 초대를 모두 삭제한다")
	// void canDeleteAllInvitationsExceptForPendingInvitations() {
	// 	// given
	// 	Member member = testDataFixture.createMember("member1");
	//
	// 	Workspace workspace1 = testDataFixture.createWorkspace("workspace 1", null, null);
	// 	Workspace workspace2 = testDataFixture.createWorkspace("workspace 2", null, null);
	// 	Workspace workspace3 = testDataFixture.createWorkspace("workspace 3", null, null);
	//
	// 	testDataFixture.createInvitation(workspace1, member, InvitationStatus.ACCEPTED);
	// 	testDataFixture.createInvitation(workspace2, member, InvitationStatus.REJECTED);
	// 	Invitation pendingInvitation = testDataFixture.createInvitation(workspace3, member, InvitationStatus.PENDING);
	//
	// 	entityManager.flush();
	// 	entityManager.clear();
	//
	// 	// when
	// 	invitationCommandService.deleteInvitations(member.getId());
	//
	// 	// then
	// 	List<Invitation> remainingInvitations = invitationRepository.findAllByMemberId(member.getId());
	// 	assertThat(remainingInvitations.get(0).getId()).isEqualTo(pendingInvitation.getId());
	// }
}
