package deprecated.com.tissue.integration.service.command;

import deprecated.com.tissue.support.helper.ServiceIntegrationTestHelper;

class WorkspaceParticipationServiceIT extends ServiceIntegrationTestHelper {

	// Workspace workspace;
	//
	// @BeforeEach
	// void setUp() {
	// 	// create workspace
	// 	workspace = testDataFixture.createWorkspace("test workspace", null, null);
	// }
	//
	// @AfterEach
	// void tearDown() {
	// 	databaseCleaner.execute();
	// }
	//
	// @Test
	// @DisplayName("유효한 워크스페이스 코드를 통해 워크스페이스에 참여할 수 있다")
	// void canJoinWorkspaceWithValidWorkspaceCode() {
	// 	// given
	// 	Member member = testDataFixture.createMember("tester");
	//
	// 	// when
	// 	WorkspaceMemberResponse response = workspaceParticipationService.joinWorkspace(
	// 		workspace.getKey(),
	// 		member.getId()
	// 	);
	//
	// 	// then
	// 	assertThat(response.workspaceCode()).isEqualTo(workspace.getKey());
	// 	assertThat(response.memberId()).isEqualTo(member.getId());
	// 	assertThat(response).isNotNull();
	// }
	//
	// @Test
	// @DisplayName("이미 워크스페이스에 참여한 멤버는 다시 참여하는 것이 불가능")
	// void testJoinWorkspace_isAlreadyMemberTrue() {
	// 	// given
	// 	Member member = testDataFixture.createMember("tester");
	//
	// 	// assume member already joined the workspace
	// 	testDataFixture.createWorkspaceMember(member, workspace, WorkspaceRole.MEMBER);
	//
	// 	// when & then
	// 	assertThatThrownBy(
	// 		() -> workspaceParticipationService.joinWorkspace(workspace.getKey(), member.getId()))
	// 		.isInstanceOf(InvalidOperationException.class);
	// }

}