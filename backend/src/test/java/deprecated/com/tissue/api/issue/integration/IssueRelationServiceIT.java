package deprecated.com.tissue.api.issue.integration;

import deprecated.com.tissue.support.helper.ServiceIntegrationTestHelper;

class IssueRelationServiceIT extends ServiceIntegrationTestHelper {

	// Workspace workspace;
	// Story sourceIssue;
	// Story targetIssue;
	// WorkspaceMember owner;
	// WorkspaceMember workspaceMember1;
	// WorkspaceMember workspaceMember2;
	// Member ownerMember;
	// Member member1;
	// Member member2;
	//
	// @BeforeEach
	// void setUp() {
	// 	// create workspace
	// 	workspace = testDataFixture.createWorkspace(
	// 		"test workspace",
	// 		null,
	// 		null
	// 	);
	//
	// 	// create member
	// 	ownerMember = testDataFixture.createMember("owner");
	// 	member1 = testDataFixture.createMember("member1");
	// 	member2 = testDataFixture.createMember("member2");
	//
	// 	// add workspace members
	// 	owner = testDataFixture.createWorkspaceMember(
	// 		ownerMember,
	// 		workspace,
	// 		WorkspaceRole.OWNER
	// 	);
	// 	workspaceMember1 = testDataFixture.createWorkspaceMember(
	// 		member1,
	// 		workspace,
	// 		WorkspaceRole.MEMBER
	// 	);
	// 	workspaceMember2 = testDataFixture.createWorkspaceMember(
	// 		member2,
	// 		workspace,
	// 		WorkspaceRole.MEMBER
	// 	);
	//
	// 	// create source issue
	// 	sourceIssue = testDataFixture.createStory(
	// 		workspace,
	// 		"source issue",
	// 		IssuePriority.MEDIUM,
	// 		LocalDateTime.now().plusDays(7)
	// 	);
	//
	// 	// create target issue
	// 	targetIssue = testDataFixture.createStory(
	// 		workspace,
	// 		"target issue",
	// 		IssuePriority.MEDIUM,
	// 		LocalDateTime.now().plusDays(7)
	// 	);
	// }
	//
	// @AfterEach
	// public void tearDown() {
	// 	databaseCleaner.execute();
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("이슈 간 관계를 설정할 수 있다(BLOCKS 관계 설정)")
	// void canCreateIssueRelationBetweenIssues() {
	// 	// given
	// 	Long requesterWorkspaceMemberId = workspaceMember1.getId();
	// 	CreateIssueRelationRequest request = new CreateIssueRelationRequest(IssueRelationType.BLOCKS);
	//
	// 	// set requester as author of source issue
	// 	// sourceIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// when - source issue BLOCKS target issue
	// 	IssueRelationResponse response = issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		targetIssue.getIssueKey(),
	// 		member1.getId(),
	// 		request
	// 	);
	//
	// 	// then
	// 	assertThat(response.workspaceCode()).isEqualTo(workspace.getKey());
	// 	assertThat(response.sourceIssueKey()).isEqualTo(sourceIssue.getIssueKey());
	// 	assertThat(response.targetIssueKey()).isEqualTo(targetIssue.getIssueKey());
	//
	// 	Issue findSourceIssue = issueRepository.findByIssueKeyAndWorkspaceCode(
	// 		sourceIssue.getIssueKey(),
	// 		workspace.getKey()
	// 	).get();
	//
	// 	IssueRelation relation = findSourceIssue.getOutgoingRelations().get(0);
	//
	// 	assertThat(relation.getRelationType()).isEqualTo(IssueRelationType.BLOCKS);
	// 	assertThat(response.issueRelationId()).isEqualTo(relation.getId());
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("이슈 관계 설정 후 소스와 타겟 이슈의 정방향, 역방향 관계의 개수는 각각 1개여야 한다")
	// void issueRelationCreation_UpdatesRelationCountCorrectly() {
	// 	// given
	// 	Long requesterWorkspaceMemberId = workspaceMember1.getId();
	// 	CreateIssueRelationRequest request = new CreateIssueRelationRequest(IssueRelationType.BLOCKS);
	// 	// sourceIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// when
	// 	IssueRelationResponse response = issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		targetIssue.getIssueKey(),
	// 		member1.getId(),
	// 		request
	// 	);
	//
	// 	// then
	// 	assertThat(response.sourceIssueKey()).isEqualTo(sourceIssue.getIssueKey());
	//
	// 	Issue findSourceIssue = issueRepository.findByIssueKeyAndWorkspaceCode(
	// 		sourceIssue.getIssueKey(),
	// 		workspace.getKey()
	// 	).get();
	//
	// 	Issue findTargetIssue = issueRepository.findByIssueKeyAndWorkspaceCode(
	// 		targetIssue.getIssueKey(),
	// 		workspace.getKey()
	// 	).get();
	//
	// 	assertThat(findSourceIssue.getOutgoingRelations()).hasSize(1);
	// 	assertThat(findSourceIssue.getIncomingRelations()).hasSize(1);
	// 	assertThat(findTargetIssue.getOutgoingRelations()).hasSize(1);
	// 	assertThat(findTargetIssue.getIncomingRelations()).hasSize(1);
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("이미 관계가 존재하는 상태에서 역방향으로 관계를 설정할 수 없다(A -> B -> A)")
	// void whenCreatingIssueRelation_CircularDependencyIsNotAllowed() {
	// 	// given
	// 	Long requesterWorkspaceMemberId = workspaceMember1.getId();
	// 	CreateIssueRelationRequest request = new CreateIssueRelationRequest(IssueRelationType.BLOCKS);
	// 	// sourceIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		targetIssue.getIssueKey(),
	// 		member1.getId(),
	// 		request
	// 	);
	//
	// 	// set requester as author of target issue
	// 	// targetIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// when & then
	// 	assertThatThrownBy(() -> issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		targetIssue.getIssueKey(),
	// 		member1.getId(),
	// 		request
	// 	)).isInstanceOf(InvalidOperationException.class);
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("이슈는 자기 자신에 대한 관계를 설정할 수 없다(A -> A)")
	// void whenCreatingIssueRelation_SelfRelationIsNotAllowed() {
	// 	// given
	// 	Long requesterWorkspaceMemberId = workspaceMember1.getId();
	// 	CreateIssueRelationRequest request = new CreateIssueRelationRequest(IssueRelationType.BLOCKS);
	// 	// sourceIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// when & then
	// 	assertThatThrownBy(() -> issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		sourceIssue.getIssueKey(),
	// 		member1.getId(),
	// 		request
	// 	)).isInstanceOf(InvalidOperationException.class);
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("타겟 이슈가 소스 이슈의 소스 이슈에 대해 관계를 설정하는 것은 불가능 하다(A -> B -> C -> A)")
	// void creatingIssueRelationWithTheSourceIssueOfTheSourceIssueIsNotAllowed_CircularDependency() {
	// 	// given
	// 	Long requesterWorkspaceMemberId = workspaceMember1.getId();
	// 	// sourceIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// create issue relation between issue A-B
	// 	issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		targetIssue.getIssueKey(),
	// 		requesterWorkspaceMemberId,
	// 		new CreateIssueRelationRequest(IssueRelationType.BLOCKS)
	// 	);
	//
	// 	// set requester as author of target issue(issue B)
	// 	// targetIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// create target issue(issue C) of issue B
	// 	Story targetIssueC = testDataFixture.createStory(
	// 		workspace,
	// 		"target issue C",
	// 		IssuePriority.MEDIUM,
	// 		LocalDateTime.now().plusDays(7)
	// 	);
	//
	// 	// set requester as author of issue C
	// 	// targetIssueC.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// create issue relation between issue B-C
	// 	issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		targetIssue.getIssueKey(),
	// 		targetIssueC.getIssueKey(),
	// 		member1.getId(),
	// 		new CreateIssueRelationRequest(IssueRelationType.BLOCKS)
	// 	);
	//
	// 	// when & then - try to create issue relation between issue C-A
	// 	assertThatThrownBy(() -> issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		targetIssueC.getIssueKey(),
	// 		sourceIssue.getIssueKey(),
	// 		requesterWorkspaceMemberId,
	// 		new CreateIssueRelationRequest(IssueRelationType.BLOCKS)
	// 	)).isInstanceOf(InvalidOperationException.class);
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("이슈 간 설정된 관계를 제거할 수 있다")
	// void canRemoveIssueRelation() {
	// 	// given
	// 	Long requesterWorkspaceMemberId = workspaceMember1.getId();
	// 	// sourceIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// when
	// 	issueRelationService.removeRelation(
	// 		workspace.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		targetIssue.getIssueKey(),
	// 		member1.getId()
	// 	);
	//
	// 	// then
	// 	Issue findSourceIssue = issueRepository.findByIssueKeyAndWorkspaceCode(
	// 		sourceIssue.getIssueKey(),
	// 		workspace.getKey()
	// 	).get();
	//
	// 	Issue findTargetIssue = issueRepository.findByIssueKeyAndWorkspaceCode(
	// 		targetIssue.getIssueKey(),
	// 		workspace.getKey()
	// 	).get();
	//
	// 	assertThat(findSourceIssue.getOutgoingRelations()).isEmpty();
	// 	assertThat(findTargetIssue.getIncomingRelations()).isEmpty();
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("이슈 간 관계 제거에 성공하면 소스 이슈와 타겟 이슈의 각 정방향, 역방향 관계는 비어있어야 한다")
	// void afterRemovingIssueRelation_OutgoingRelationsAndIncomingRelationsMustBeEmpty() {
	// 	// given
	// 	Long requesterWorkspaceMemberId = workspaceMember1.getId();
	// 	// sourceIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// when
	// 	issueRelationService.removeRelation(
	// 		workspace.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		targetIssue.getIssueKey(),
	// 		member1.getId()
	// 	);
	//
	// 	// then
	// 	Issue findSourceIssue = issueRepository.findByIssueKeyAndWorkspaceCode(
	// 		sourceIssue.getIssueKey(),
	// 		workspace.getKey()
	// 	).get();
	//
	// 	Issue findTargetIssue = issueRepository.findByIssueKeyAndWorkspaceCode(
	// 		targetIssue.getIssueKey(),
	// 		workspace.getKey()
	// 	).get();
	//
	// 	assertThat(findSourceIssue.getOutgoingRelations()).isEmpty();
	// 	assertThat(findSourceIssue.getIncomingRelations()).isEmpty();
	// 	assertThat(findTargetIssue.getOutgoingRelations()).isEmpty();
	// 	assertThat(findTargetIssue.getIncomingRelations()).isEmpty();
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("직접적인 순환 참조가 있는 경우 순환 참조 검증기에서 예외를 던진다(A -> B -> A)")
	// void circularDependencyCheckerThrowsException_IfDirectCircularDependencyExists() {
	// 	// given
	// 	Long requesterWorkspaceMemberId = workspaceMember1.getId();
	//
	// 	// set requester as author of source issue
	// 	// sourceIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// source issue BLOCKS target issue (A -> B)
	// 	IssueRelationResponse response = issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		targetIssue.getIssueKey(),
	// 		member1.getId(),
	// 		new CreateIssueRelationRequest(IssueRelationType.BLOCKS)
	// 	);
	//
	// 	// when & then - validate circular dependency (B -> A)
	// 	assertThatThrownBy(
	// 		() -> circularDependencyValidator.validateNoCircularDependency(targetIssue, sourceIssue))
	// 		.isInstanceOf(InvalidOperationException.class);
	// }
	//
	// @Test
	// @Transactional
	// @DisplayName("간접적인 순환 참조가 있는 경우 순환 참조 검증기에서 예외를 던진다(given A -> B, C -> A when B -> C)")
	// void circularDependencyCheckerThrowsException_IfInDirectCircularDependencyExists() {
	// 	// given
	// 	Long requesterWorkspaceMemberId = workspaceMember1.getId();
	//
	// 	// set requester as author of source issue
	// 	// sourceIssue.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// source issue BLOCKS target issue (A -> B)
	// 	issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		targetIssue.getIssueKey(),
	// 		requesterWorkspaceMemberId,
	// 		new CreateIssueRelationRequest(IssueRelationType.BLOCKS)
	// 	);
	//
	// 	Issue issueC = testDataFixture.createStory(
	// 		workspace,
	// 		"issue C",
	// 		IssuePriority.MEDIUM,
	// 		LocalDateTime.now().plusDays(7)
	// 	);
	//
	// 	// issueC.updateCreatedByWorkspaceMember(requesterWorkspaceMemberId);
	//
	// 	// issue C BLOCKS source issue (C -> A)
	// 	issueRelationService.createRelation(
	// 		workspace.getKey(),
	// 		issueC.getKey(),
	// 		sourceIssue.getIssueKey(),
	// 		requesterWorkspaceMemberId,
	// 		new CreateIssueRelationRequest(IssueRelationType.BLOCKS)
	// 	);
	//
	// 	// when & then - validate circular dependency (B -> C)
	// 	assertThatThrownBy(
	// 		() -> circularDependencyValidator.validateNoCircularDependency(targetIssue, issueC))
	// 		.isInstanceOf(InvalidOperationException.class);
	// }
}