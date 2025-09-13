package deprecated.com.tissue.integration.service.query;

import deprecated.com.tissue.support.helper.ServiceIntegrationTestHelper;

class WorkspaceFinderIT extends ServiceIntegrationTestHelper {

	// Member member1;
	// Workspace workspace1;
	// Workspace workspace2;
	// WorkspaceMember workspaceOneMember;
	// WorkspaceMember workspaceTwoMember;
	//
	// @BeforeEach
	// void setup() {
	// 	// create member
	// 	member1 = testDataFixture.createMember("member1");
	//
	// 	// create workspace 1
	// 	workspace1 = testDataFixture.createWorkspace("workspace1", null, null);
	//
	// 	// create workspace 2
	// 	workspace2 = testDataFixture.createWorkspace("workspace2", null, null);
	//
	// 	// member joins workspace1, workspace2
	// 	workspaceOneMember = testDataFixture.createWorkspaceMember(member1, workspace1, WorkspaceRole.MEMBER);
	// 	workspaceTwoMember = testDataFixture.createWorkspaceMember(member1, workspace2, WorkspaceRole.MEMBER);
	// }
	//
	// @AfterEach
	// void tearDown() {
	// 	databaseCleaner.execute();
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("해당 워크스페이스에 참여하고 있는 멤버는, 워크스페이스의 코드로 상세 정보를 조회할 수 있다")
	// void canGetWorkspaceDetailWithValidWorkspaceCode() {
	// 	// given
	// 	Member member = testDataFixture.createMember("tester");
	//
	// 	Workspace workspace = testDataFixture.createWorkspace(
	// 		"test workspace",
	// 		null,
	// 		null
	// 	);
	//
	// 	WorkspaceMember workspaceMember = testDataFixture.createWorkspaceMember(
	// 		member,
	// 		workspace,
	// 		WorkspaceRole.MEMBER
	// 	);
	//
	// 	// when
	// 	WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail(workspace.getKey());
	//
	// 	// then
	// 	assertThat(response.getCode()).isEqualTo(workspace.getKey());
	// 	assertThat(response.getName()).isEqualTo("test workspace");
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("유효하지 않은 코드로 조회할 수 없다")
	// void cannotGetWorkspaceDetailWithInvalidCode() {
	// 	// when & then
	// 	assertThatThrownBy(() -> workspaceQueryService.getWorkspaceDetail("INVALIDCODE"))
	// 		.isInstanceOf(WorkspaceNotFoundException.class);
	// }
}
