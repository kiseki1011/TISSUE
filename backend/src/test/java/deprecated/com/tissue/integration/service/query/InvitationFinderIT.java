package deprecated.com.tissue.integration.service.query;

import deprecated.com.tissue.support.helper.ServiceIntegrationTestHelper;

class InvitationFinderIT extends ServiceIntegrationTestHelper {

	// @AfterEach
	// public void tearDown() {
	// 	databaseCleaner.execute();
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("멤버가 받은 초대(Invitation) 목록을 상태(InvitationStatus)를 조건으로 조회할 수 있다")
	// void canQueryInvitationsByStatus() {
	// 	// given
	// 	Member member = testDataFixture.createMember("tester");
	//
	// 	Workspace workspace1 = testDataFixture.createWorkspace("workspace 1", null, null);
	// 	Workspace workspace2 = testDataFixture.createWorkspace("workspace 2", null, null);
	//
	// 	Invitation pendingInvitation = testDataFixture.createInvitation(workspace1, member, InvitationStatus.PENDING);
	// 	Invitation acceptedInvitation = testDataFixture.createInvitation(workspace2, member, InvitationStatus.ACCEPTED);
	//
	// 	InvitationSearchCondition searchCondition = new InvitationSearchCondition(
	// 		List.of(InvitationStatus.PENDING, InvitationStatus.ACCEPTED)
	// 	);
	// 	Pageable pageable = PageRequest.of(
	// 		0,
	// 		20,
	// 		Sort.by(Sort.Direction.DESC, "createdDate")
	// 	);
	//
	// 	// when
	// 	Page<InvitationDetail> result = invitationQueryService.getInvitations(
	// 		member.getId(),
	// 		searchCondition,
	// 		pageable
	// 	);
	//
	// 	// then
	// 	assertThat(result.getTotalElements()).isEqualTo(2);
	// 	assertThat(result.getContent())
	// 		.extracting("invitationId", "status")
	// 		.contains(
	// 			tuple(pendingInvitation.getId(), InvitationStatus.PENDING),
	// 			tuple(acceptedInvitation.getId(), InvitationStatus.ACCEPTED)
	// 		);
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("초대 조회 시, 상태를 지정하지 않으면 PENDING 상태의 초대만 조회된다")
	// void whenQueryInvitations_IfNoStatusIsProvided_DefaultStatusConditionIsPending() {
	// 	// given
	// 	Member member = testDataFixture.createMember("tester");
	//
	// 	Workspace workspace1 = testDataFixture.createWorkspace("workspace 1", null, null);
	// 	Workspace workspace2 = testDataFixture.createWorkspace("workspace 2", null, null);
	//
	// 	Invitation pendingInvitation = testDataFixture.createInvitation(workspace1, member, InvitationStatus.PENDING);
	// 	Invitation acceptedInvitation = testDataFixture.createInvitation(workspace2, member, InvitationStatus.ACCEPTED);
	//
	// 	InvitationSearchCondition searchCondition = new InvitationSearchCondition();
	// 	Pageable pageable = PageRequest.of(
	// 		0,
	// 		20,
	// 		Sort.by(Sort.Direction.DESC, "createdDate")
	// 	);
	//
	// 	// when
	// 	Page<InvitationDetail> result = invitationQueryService.getInvitations(
	// 		member.getId(),
	// 		searchCondition,
	// 		pageable
	// 	);
	//
	// 	// then
	// 	assertThat(result.getTotalElements()).isEqualTo(1);
	// 	assertThat(result.getContent().get(0).invitationId()).isEqualTo(pendingInvitation.getId());
	// 	assertThat(result.getContent().get(0).status()).isEqualTo(InvitationStatus.PENDING);
	// }

}
