package com.tissue.api.issue.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.exception.CircularDependencyException;
import com.tissue.api.issue.exception.DuplicateIssueRelationException;
import com.tissue.api.issue.exception.SelfReferenceNotAllowedException;
import com.tissue.api.issue.presentation.dto.request.CreateIssueRelationRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateStoryRequest;
import com.tissue.api.issue.presentation.dto.response.CreateIssueRelationResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveIssueRelationResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateStoryResponse;
import com.tissue.api.member.presentation.dto.response.SignupMemberResponse;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

class IssueRelationCommandServiceIT extends ServiceIntegrationTestHelper {

	String workspaceCode;
	String sourceIssueKey;
	String targetIssueKey;

	@BeforeEach
	void setUp() {
		// 테스트 멤버 testUser, testUser2, testUser3 생성
		SignupMemberResponse testUser = memberFixture.createMember("testuser", "test@test.com");
		SignupMemberResponse testUser2 = memberFixture.createMember("testuser2", "test2@test.com");
		SignupMemberResponse testUser3 = memberFixture.createMember("testuser3", "test3@test.com");

		// testUser가 Workspace 생성
		CreateWorkspaceResponse createWorkspace = workspaceFixture.createWorkspace(testUser.memberId());

		workspaceCode = createWorkspace.code();

		// testUser2, testUser3가 생성한 Workspace에 참가
		workspaceParticipationCommandService.joinWorkspace(workspaceCode, testUser2.memberId());
		workspaceParticipationCommandService.joinWorkspace(workspaceCode, testUser3.memberId());

		// Source Issue 생성
		CreateStoryRequest createSourceStory = CreateStoryRequest.builder()
			.title("Source Story Issue")
			.content("Source Story Issue")
			.userStory("Source Story Issue")
			.build();

		CreateStoryResponse sourceIssue = (CreateStoryResponse)issueCommandService.createIssue(workspaceCode,
			createSourceStory);

		// testUser2가 Source Issue에 작업자로 참여
		assigneeCommandService.addAssignee(workspaceCode, sourceIssue.issueKey(), 2L);

		// Target Issue 생성
		CreateStoryRequest createTargetStory = CreateStoryRequest.builder()
			.title("Target Story Issue")
			.content("Target Story Issue")
			.userStory("Target Story Issue")
			.build();

		CreateStoryResponse targetIssue = (CreateStoryResponse)issueCommandService.createIssue(workspaceCode,
			createTargetStory);

		sourceIssueKey = sourceIssue.issueKey();
		targetIssueKey = targetIssue.issueKey();
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@Transactional
	@DisplayName("이슈 간 관계 설정을 성공하면 성공 응답을 반환 한다")
	void createIssueRelation_success_returnsCreateIssueRelationResponse() {
		// given
		Long requesterWorkspaceMemberId = 2L;
		CreateIssueRelationRequest request = new CreateIssueRelationRequest(IssueRelationType.BLOCKS);

		// when - 소스 이슈가 타겟 이슈를 BLOCKS
		CreateIssueRelationResponse response = issueRelationCommandService.createRelation(
			workspaceCode,
			sourceIssueKey,
			targetIssueKey,
			requesterWorkspaceMemberId,
			request
		);

		// then
		assertThat(response.relationType()).isEqualTo(IssueRelationType.BLOCKS);
		assertThat(response.sourceIssueKey()).isEqualTo(sourceIssueKey);
	}

	@Test
	@Transactional
	@DisplayName("이슈 간 관계 설정을 성공하면 소스 이슈와 타겟 이슈의 각 정방향, 역방향 관계의 수는 1개 이어야 한다")
	void createIssueRelation_success_outgoingRelationSize1_incomingRelationsSize1() {
		// given
		Long requesterWorkspaceMemberId = 2L;
		CreateIssueRelationRequest request = new CreateIssueRelationRequest(IssueRelationType.BLOCKS);

		// when
		CreateIssueRelationResponse response = issueRelationCommandService.createRelation(
			workspaceCode,
			sourceIssueKey,
			targetIssueKey,
			requesterWorkspaceMemberId,
			request
		);

		// then
		assertThat(response.relationType()).isEqualTo(IssueRelationType.BLOCKS);
		assertThat(response.sourceIssueKey()).isEqualTo(sourceIssueKey);

		Issue sourceIssue = issueRepository.findByIssueKeyAndWorkspaceCode(sourceIssueKey, workspaceCode).orElseThrow();
		Issue targetIssue = issueRepository.findByIssueKeyAndWorkspaceCode(targetIssueKey, workspaceCode).orElseThrow();

		assertThat(sourceIssue.getOutgoingRelations()).hasSize(1);
		assertThat(sourceIssue.getIncomingRelations()).hasSize(1);
		assertThat(targetIssue.getOutgoingRelations()).hasSize(1);
		assertThat(targetIssue.getIncomingRelations()).hasSize(1);
	}

	@Test
	@Transactional
	@DisplayName("타겟 이슈에서 반대로 소스 이슈에 대한 관계를 설정하려고 하면 예외가 발생한다(A -> B -> A)")
	void createIssueRelation_sourceToTargetToSource_circularDependency() {
		// given
		Long requesterWorkspaceMemberId = 2L;
		CreateIssueRelationRequest request = new CreateIssueRelationRequest(IssueRelationType.BLOCKS);

		issueRelationCommandService.createRelation(
			workspaceCode,
			sourceIssueKey,
			targetIssueKey,
			requesterWorkspaceMemberId,
			request
		);

		// 타겟 이슈에 작업자로 참여
		assigneeCommandService.addAssignee(
			workspaceCode,
			targetIssueKey,
			2L
		);

		// when & then
		assertThatThrownBy(() -> issueRelationCommandService.createRelation(
			workspaceCode,
			targetIssueKey,
			sourceIssueKey,
			requesterWorkspaceMemberId,
			request
		)).isInstanceOf(DuplicateIssueRelationException.class);
	}

	@Test
	@Transactional
	@DisplayName("소스 이슈가 스스로에 대해 관계를 설정하려고 하면 예외가 발생한다(A -> A)")
	void createIssueRelation_sameIssue_throwsException() {
		// given
		Long requesterWorkspaceMemberId = 2L;
		CreateIssueRelationRequest request = new CreateIssueRelationRequest(IssueRelationType.BLOCKS);

		// when & then
		assertThatThrownBy(() -> issueRelationCommandService.createRelation(
			workspaceCode,
			sourceIssueKey,
			sourceIssueKey,
			requesterWorkspaceMemberId,
			request
		)).isInstanceOf(SelfReferenceNotAllowedException.class);
	}

	@Test
	@Transactional
	@DisplayName("상위의 타겟 이슈에서 하위의 소스 이슈 간 관계를 설정하려면 예외가 발생한다(A -> B -> C -> A)")
	void createIssueRelation_sourceToTargetToTargetToSource_circularDependency() {
		// given
		Long requesterWorkspaceMemberId = 2L;

		// 이슈 A-B 관계 설정
		issueRelationCommandService.createRelation(
			workspaceCode,
			sourceIssueKey,
			targetIssueKey,
			requesterWorkspaceMemberId,
			new CreateIssueRelationRequest(IssueRelationType.BLOCKS)
		);

		// 이슈 B에 참여
		assigneeCommandService.addAssignee(
			workspaceCode,
			targetIssueKey,
			2L
		);

		// 이슈 B의 타겟 이슈 C 생성
		CreateStoryRequest createTargetIssueC = CreateStoryRequest.builder()
			.title("Target Story Issue C")
			.content("Target Story Issue C")
			.userStory("Target Story Issue C")
			.build();

		CreateStoryResponse targetIssueC = (CreateStoryResponse)issueCommandService.createIssue(workspaceCode,
			createTargetIssueC);

		// 이슈 C에 참여
		assigneeCommandService.addAssignee(
			workspaceCode,
			targetIssueC.issueKey(),
			2L
		);

		// 이슈 B-C 관계 설정
		issueRelationCommandService.createRelation(
			workspaceCode,
			targetIssueKey,
			targetIssueC.issueKey(),
			requesterWorkspaceMemberId,
			new CreateIssueRelationRequest(IssueRelationType.BLOCKS)
		);

		// when & then - C-A 관계 시도
		assertThatThrownBy(() -> issueRelationCommandService.createRelation(
			workspaceCode,
			targetIssueC.issueKey(),
			sourceIssueKey,
			requesterWorkspaceMemberId,
			new CreateIssueRelationRequest(IssueRelationType.BLOCKS)
		)).isInstanceOf(CircularDependencyException.class);
	}

	@Test
	@Transactional
	@DisplayName("이슈 간 관계 제거에 성공하면 성공 응답을 반환한다")
	void removeIssueRelation_success_returnsRemoveIssueRelationResponse() {
		// given
		Long requesterWorkspaceMemberId = 2L;

		// when
		RemoveIssueRelationResponse response = issueRelationCommandService.removeRelation(
			workspaceCode,
			sourceIssueKey,
			targetIssueKey,
			requesterWorkspaceMemberId
		);

		// then
		assertThat(response.sourceIssueKey()).isEqualTo(sourceIssueKey);
		assertThat(response.targetIssueKey()).isEqualTo(targetIssueKey);
	}

	@Test
	@Transactional
	@DisplayName("이슈 간 관계 제거에 성공하면 소스 이슈와 타겟 이슈의 각 정방향, 역방향 관계는 비어있어야 한다")
	void createIssueRelation_success_outgoingRelationEmpty_incomingRelationsEmpty() {
		// given
		Long requesterWorkspaceMemberId = 2L;

		// when
		RemoveIssueRelationResponse response = issueRelationCommandService.removeRelation(
			workspaceCode,
			sourceIssueKey,
			targetIssueKey,
			requesterWorkspaceMemberId
		);

		// then
		assertThat(response.sourceIssueKey()).isEqualTo(sourceIssueKey);
		assertThat(response.targetIssueKey()).isEqualTo(targetIssueKey);

		Issue sourceIssue = issueRepository.findByIssueKeyAndWorkspaceCode(sourceIssueKey, workspaceCode).orElseThrow();
		Issue targetIssue = issueRepository.findByIssueKeyAndWorkspaceCode(targetIssueKey, workspaceCode).orElseThrow();
		assertThat(sourceIssue.getOutgoingRelations()).isEmpty();
		assertThat(sourceIssue.getIncomingRelations()).isEmpty();
		assertThat(targetIssue.getOutgoingRelations()).isEmpty();
		assertThat(targetIssue.getIncomingRelations()).isEmpty();
	}
}