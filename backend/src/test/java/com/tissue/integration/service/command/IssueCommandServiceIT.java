package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateStoryRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateSubTaskRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateTaskRequest;
import com.tissue.api.issue.presentation.dto.request.update.UpdateStoryRequest;
import com.tissue.api.issue.presentation.dto.response.AssignParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateStoryResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateTaskResponse;
import com.tissue.api.issue.presentation.dto.response.delete.DeleteIssueResponse;
import com.tissue.api.issue.presentation.dto.response.update.UpdateStoryResponse;
import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class IssueCommandServiceIT extends ServiceIntegrationTestHelper {

	Workspace workspace;
	WorkspaceMember owner;
	WorkspaceMember workspaceMember1;
	WorkspaceMember workspaceMember2;

	@BeforeEach
	void setUp() {
		// create workspace
		workspace = testDataFixture.createWorkspace(
			"test workspace",
			null,
			null
		);

		// create member
		Member ownerMember = testDataFixture.createMember("owner");
		Member member1 = testDataFixture.createMember("member1");
		Member member2 = testDataFixture.createMember("member2");

		// add workspace members
		owner = testDataFixture.createWorkspaceMember(
			ownerMember,
			workspace,
			WorkspaceRole.OWNER
		);
		workspaceMember1 = testDataFixture.createWorkspaceMember(
			member1,
			workspace,
			WorkspaceRole.MEMBER
		);
		workspaceMember2 = testDataFixture.createWorkspaceMember(
			member2,
			workspace,
			WorkspaceRole.MEMBER
		);
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@Transactional
	@DisplayName("워크스페이스 멤버는 TASK 타입 이슈를 생성할 수 있다")
	void canCreateTaskIssue() {
		// given
		CreateTaskRequest request = CreateTaskRequest.builder()
			.title("test issue")
			.content("test content")
			.priority(IssuePriority.MEDIUM)
			.dueAt(LocalDateTime.now())
			.build();

		// when
		CreateTaskResponse response = (CreateTaskResponse)issueCommandService.createIssue(
			workspace.getCode(),
			request
		);

		// then
		assertThat(response.getType()).isEqualTo(IssueType.TASK);
		assertThat(response.title()).isEqualTo("test issue");

		Issue findIssue = issueRepository.findById(response.issueId()).orElseThrow();
		assertThat(findIssue.getWorkspaceCode()).isEqualTo(workspace.getCode());
	}

	@Test
	@Transactional
	@DisplayName("특정 이슈를 부모로 지정하여 이슈를 생성하는 것이 가능하다")
	void canCreateIssueWithParent() {
		// given
		Issue parentIssue = testDataFixture.createEpic(
			workspace,
			"parent issue (EPIC type)",
			IssuePriority.MEDIUM,
			null
		);

		CreateStoryRequest request = CreateStoryRequest.builder()
			.title("child story")
			.content("child story")
			.priority(IssuePriority.MEDIUM)
			.dueAt(LocalDateTime.now())
			.parentIssueKey(parentIssue.getIssueKey())
			.userStory("user story")
			.acceptanceCriteria("acceptance criteria")
			.build();

		// when
		CreateStoryResponse response = (CreateStoryResponse)issueCommandService.createIssue(
			workspace.getCode(),
			request
		);

		// then
		Issue savedIssue = issueRepository.findById(response.issueId()).orElseThrow();

		assertThat(response.title()).isEqualTo("child story");
		assertThat(savedIssue.getParentIssue().getId()).isEqualTo(parentIssue.getId());
	}

	@Test
	@Transactional
	@DisplayName("STORY 타입 이슈 생성 시, TASK 타입 이슈를 부모로 지정할 수 없다")
	void cannotCreateStoryIfParentIsTask() {
		// given
		Issue parentIssue = testDataFixture.createTask(
			workspace,
			"parent issue (TASK type)",
			IssuePriority.MEDIUM,
			null
		);

		CreateStoryRequest request = CreateStoryRequest.builder()
			.title("child story")
			.content("child story")
			.priority(IssuePriority.MEDIUM)
			.dueAt(LocalDateTime.now())
			.parentIssueKey(parentIssue.getIssueKey())
			.userStory("user story")
			.acceptanceCriteria("acceptance criteria")
			.build();

		// when & then
		assertThatThrownBy(() -> issueCommandService.createIssue(workspace.getCode(), request))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@Transactional
	@DisplayName("SUB_TASK 이슈 생성 시, EPIC 타입 이슈를 부모로 지정할 수 없다")
	void cannotCreateSubTaskWithEpicParent() {
		// given
		Issue parentIssue = testDataFixture.createEpic(
			workspace,
			"parent issue (EPIC type)",
			IssuePriority.MEDIUM,
			null
		);

		CreateSubTaskRequest request = CreateSubTaskRequest.builder()
			.title("child subtask")
			.content("child subtask")
			.priority(IssuePriority.MEDIUM)
			.dueAt(LocalDateTime.now())
			.parentIssueKey(parentIssue.getIssueKey())
			.build();

		// when & then
		assertThatThrownBy(() -> issueCommandService.createIssue(workspace.getCode(), request))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@Transactional
	@DisplayName("같은 위계의 타입을 가진 이슈를 부모로 지정할 수 없다")
	void cannotCreateIssueWithParentIfParentIsSameHierarchyAsChild() {
		// given
		Issue parentIssue = testDataFixture.createTask(
			workspace,
			"parent issue (TASK type)",
			IssuePriority.MEDIUM,
			null
		);

		CreateTaskRequest request = CreateTaskRequest.builder()
			.title("child task")
			.content("child task")
			.priority(IssuePriority.MEDIUM)
			.dueAt(LocalDateTime.now())
			.parentIssueKey(parentIssue.getIssueKey())
			.build();

		// when & then
		assertThatThrownBy(() -> issueCommandService.createIssue(workspace.getCode(), request))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@Transactional
	@DisplayName("제일 처음 생성된 이슈의 이슈키는 'ISSUE-1'이어야 한다")
	void whenFirstIssueIsCreatedIssueKeyMustBe_ISSUE_1() {
		// given
		CreateTaskRequest request = CreateTaskRequest.builder()
			.title("task issue")
			.content("task issue")
			.priority(IssuePriority.HIGH)
			.dueAt(LocalDateTime.now())
			.build();

		// when
		CreateTaskResponse response = (CreateTaskResponse)issueCommandService.createIssue(
			workspace.getCode(),
			request
		);

		// then
		Issue savedIssue = issueRepository.findById(response.issueId()).orElseThrow();
		assertThat(savedIssue.getIssueKey()).isEqualTo("ISSUE-1");
		assertThat(savedIssue.getWorkspaceCode()).isEqualTo(workspace.getCode());
	}

	@Test
	@Transactional
	@DisplayName("이슈키 prefix를 설정하지 않은 경우, 두 번째 이슈의 이슈키는 'ISSUE-2'이어야 한다")
	void whenSecondIssueIsCreatedTheIssueKeyMustBe_ISSUE_2() {
		// given
		Issue firstIssue = testDataFixture.createTask(
			workspace,
			"first issue (TASK type)",
			IssuePriority.MEDIUM,
			null
		);

		CreateTaskRequest request = CreateTaskRequest.builder()
			.title("second issue (TASK type)")
			.content("second issue (TASK type)")
			.priority(IssuePriority.MEDIUM)
			.dueAt(LocalDateTime.now())
			.build();

		// when
		CreateTaskResponse response = (CreateTaskResponse)issueCommandService.createIssue(
			workspace.getCode(),
			request
		);

		// then
		assertThat(response.getType()).isEqualTo(IssueType.TASK);
		assertThat(response.title()).isEqualTo("second issue (TASK type)");

		Issue secondIssue = issueRepository.findById(response.issueId()).orElseThrow();
		assertThat(firstIssue.getIssueKey()).isEqualTo("ISSUE-1");
		assertThat(secondIssue.getIssueKey()).isEqualTo("ISSUE-2");
	}

	@Test
	@Transactional
	@DisplayName("이슈의 작성자는 본인이 작성한 이슈를 업데이트할 수 있다")
	void issueAuthorCanEditIssue() {
		// given
		Issue issue = testDataFixture.createStory(
			workspace,
			"test issue (STORY type)",
			IssuePriority.MEDIUM,
			null
		);

		issue.updateCreatedByWorkspaceMember(workspaceMember1.getId());

		UpdateStoryRequest request = UpdateStoryRequest.builder()
			.title("updated issue")
			.content("updated issue")
			.priority(IssuePriority.HIGH)
			.dueAt(LocalDateTime.now())
			.userStory("updated issue")
			.acceptanceCriteria("updated issue")
			.build();

		// when
		UpdateStoryResponse response = (UpdateStoryResponse)issueCommandService.updateIssue(
			workspace.getCode(),
			issue.getIssueKey(),
			workspaceMember1.getId(),
			request
		);

		// then
		assertThat(response.issueKey()).isEqualTo(issue.getIssueKey());
		assertThat(response.title()).isEqualTo("updated issue");
	}

	@Test
	@Transactional
	@DisplayName("요청의 이슈 타입과 업데이트를 위해 조회한 이슈 타입은 일치해야 한다")
	void updateIssueTypeMismatchIsNotAllowed() {
		// given
		Issue issue = testDataFixture.createTask(
			workspace,
			"test issue (TASK type)",
			IssuePriority.MEDIUM,
			null
		);

		issue.updateCreatedByWorkspaceMember(workspaceMember1.getId());

		UpdateStoryRequest request = UpdateStoryRequest.builder()
			.title("updated issue")
			.content("updated issue")
			.priority(IssuePriority.HIGH)
			.dueAt(LocalDateTime.now())
			.userStory("updated issue")
			.acceptanceCriteria("updated issue")
			.build();

		// when & then
		assertThatThrownBy(
			() -> issueCommandService.updateIssue(workspace.getCode(), issue.getIssueKey(), workspaceMember1.getId(),
				request))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@Transactional
	@DisplayName("이슈 작성자는 이슈를 삭제할 수 있다")
	void issueAuthorCanDeleteIssue() {
		// given
		Issue issue = testDataFixture.createStory(
			workspace,
			"test issue (STORY type)",
			IssuePriority.MEDIUM,
			null
		);

		issue.updateCreatedByWorkspaceMember(workspaceMember1.getId());

		// when
		DeleteIssueResponse response = issueCommandService.deleteIssue(
			workspace.getCode(),
			issue.getIssueKey(),
			workspaceMember1.getId()
		);

		// then
		assertThat(response.issueKey()).isEqualTo(issue.getIssueKey());
	}

	@Test
	@Transactional
	@DisplayName("SUB_TASK의 부모 이슈는 삭제할 수 없다")
	void cannotDeleteParentOfSubTask() {
		// given
		Issue parentIssue = testDataFixture.createStory(
			workspace,
			"parent issue (STORY type)",
			IssuePriority.MEDIUM,
			null
		);

		parentIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());

		Issue childIssue = testDataFixture.createSubTask(
			workspace,
			"child issue (SUBTASK type)",
			IssuePriority.MEDIUM,
			null
		);
		childIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());
		childIssue.updateParentIssue(parentIssue);

		// when & then
		assertThatThrownBy(
			() -> issueCommandService.deleteIssue(workspace.getCode(), parentIssue.getIssueKey(),
				workspaceMember1.getId()))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@Transactional
	@DisplayName("STORY의 부모로 EPIC을 등록할 수 있다")
	void canAssignEpicAsParentIssueOfStory() {
		// given
		Issue parentIssue = testDataFixture.createEpic(
			workspace,
			"parent issue (EPIC type)",
			IssuePriority.MEDIUM,
			null
		);
		parentIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());

		Issue childIssue = testDataFixture.createStory(
			workspace,
			"child issue (STORY type)",
			IssuePriority.MEDIUM,
			null
		);
		childIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());

		// when
		AssignParentIssueResponse assignParentResponse = issueCommandService.assignParentIssue(
			workspace.getCode(),
			childIssue.getIssueKey(),
			workspaceMember1.getId(),
			new AssignParentIssueRequest(parentIssue.getIssueKey())
		);

		// then
		assertThat(assignParentResponse.parentIssueKey()).isEqualTo(parentIssue.getIssueKey());
		assertThat(assignParentResponse.parentIssueId()).isEqualTo(parentIssue.getId());
	}

	@Test
	@Transactional
	@DisplayName("이슈의 부모를 변경할 수 있다")
	void canChangeParentIssueOfIssue() {
		// given
		Issue parentIssue = testDataFixture.createEpic(
			workspace,
			"parent issue (EPIC type)",
			IssuePriority.MEDIUM,
			null
		);
		parentIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());

		Issue childIssue = testDataFixture.createStory(
			workspace,
			"child issue (STORY type)",
			IssuePriority.MEDIUM,
			null
		);
		childIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());
		childIssue.updateParentIssue(parentIssue);

		// 변경할 부모 이슈 생성
		Issue newParentIssue = testDataFixture.createEpic(
			workspace,
			"new parent issue (EPIC type)",
			IssuePriority.MEDIUM,
			null
		);
		newParentIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());

		// when
		AssignParentIssueResponse assignParentResponse = issueCommandService.assignParentIssue(
			workspace.getCode(),
			childIssue.getIssueKey(),
			workspaceMember1.getId(),
			new AssignParentIssueRequest(newParentIssue.getIssueKey())
		);

		// then
		assertThat(assignParentResponse.parentIssueKey()).isEqualTo(newParentIssue.getIssueKey());
		assertThat(assignParentResponse.parentIssueId()).isEqualTo(newParentIssue.getId());
	}

	@Test
	@Transactional
	@DisplayName("STORY의 부모 이슈인 EPIC에 대한 부모 관계를 해제할 수 있다")
	void canRemoveParentRelationship() {
		// given
		Issue parentIssue = testDataFixture.createEpic(
			workspace,
			"parent issue (EPIC type)",
			IssuePriority.MEDIUM,
			null
		);

		parentIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());

		Issue childIssue = testDataFixture.createStory(
			workspace,
			"child issue (STORY type)",
			IssuePriority.MEDIUM,
			null
		);

		childIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());
		childIssue.updateParentIssue(parentIssue);

		// when
		RemoveParentIssueResponse response = issueCommandService.removeParentIssue(
			workspace.getCode(),
			childIssue.getIssueKey(),
			workspaceMember1.getId()
		);

		// then
		assertThat(response.issueKey()).isEqualTo(childIssue.getIssueKey());

		Issue updatedIssue = issueRepository.findById(response.issueId()).orElseThrow();
		assertThat(updatedIssue.getParentIssue()).isNull();
	}

	@Test
	@Transactional
	@DisplayName("SUBTASK의 부모 이슈는 해제할 수 없다")
	void cannotRemoveParentOfSubTask() {
		// given
		Issue parentIssue = testDataFixture.createTask(
			workspace,
			"parent issue (Task type)",
			IssuePriority.MEDIUM,
			null
		);

		parentIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());

		Issue childIssue = testDataFixture.createSubTask(
			workspace,
			"child issue (SUB_TASK type)",
			IssuePriority.MEDIUM,
			null
		);

		childIssue.updateCreatedByWorkspaceMember(workspaceMember1.getId());
		childIssue.updateParentIssue(parentIssue);

		// when & then
		assertThatThrownBy(() -> issueCommandService.removeParentIssue(
			workspace.getCode(),
			childIssue.getIssueKey(),
			workspaceMember1.getId()
		)).isInstanceOf(InvalidOperationException.class);
	}

	//
	// @Test
	// @DisplayName("이슈 상태 업데이트를 성공하면 이슈 상태 업데이트 응답을 반환한다")
	// void updateIssueStatus_success_returnUpdateStatusResponse() {
	// 	// given
	// 	CreateIssueRequest createRequest = new CreateIssueRequest(
	// 		IssueType.TASK,
	// 		"Test Issue",
	// 		"Test issue content",
	// 		IssuePriority.HIGH,
	// 		LocalDate.now(),
	// 		null
	// 	);
	// 	issueCommandService.createIssue(workspace.getCode(), createRequest);
	//
	// 	UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.IN_PROGRESS);
	//
	// 	// when
	// 	UpdateStatusResponse response = issueCommandService.updateIssueStatus(1L, workspace.getCode(), updateStatusRequest);
	//
	// 	// then
	// 	assertThat(response.issueId()).isEqualTo(1L);
	// 	assertThat(response.status()).isEqualTo(IssueStatus.IN_PROGRESS);
	// }
	//
	// @Test
	// @DisplayName("이슈 상태를 IN_REVIEW로 직접 업데이트 시도하는 경우 예외가 발생한다")
	// void updateIssueStatus_fails_ifUpdateDirectlyToInReview() {
	// 	// given
	// 	CreateIssueRequest createRequest = new CreateIssueRequest(
	// 		IssueType.TASK,
	// 		"Test Issue",
	// 		"Test issue content",
	// 		IssuePriority.HIGH,
	// 		LocalDate.now(),
	// 		null
	// 	);
	// 	issueCommandService.createIssue(workspace.getCode(), createRequest);
	//
	// 	UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.IN_REVIEW);
	//
	// 	// when & then
	// 	assertThatThrownBy(() -> issueCommandService.updateIssueStatus(1L, workspace.getCode(), updateStatusRequest))
	// 		.isInstanceOf(DirectUpdateToInReviewException.class);
	// }
	//
	// @Test
	// @DisplayName("이슈 상태를 처음으로 IN_PROGRESS로 업데이트하는 경우, startedAt이 현재 날짜와 시간으로 기록된다")
	// void updateIssueStatus_toInProgress_startedAtIsRecorded() {
	// 	// given
	// 	CreateIssueRequest createRequest = new CreateIssueRequest(
	// 		IssueType.TASK,
	// 		"Test Issue",
	// 		"Test issue content",
	// 		IssuePriority.HIGH,
	// 		LocalDate.now(),
	// 		null
	// 	);
	// 	issueCommandService.createIssue(workspace.getCode(), createRequest);
	//
	// 	UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.IN_PROGRESS);
	//
	// 	// when
	// 	LocalDateTime timeBeforeUpdate = LocalDateTime.now();
	// 	issueCommandService.updateIssueStatus(1L, workspace.getCode(), updateStatusRequest);
	//
	// 	// then
	// 	Issue issue = issueRepository.findById(1L).orElseThrow();
	//
	// 	assertThat(issue.getStartedAt()).isAfter(timeBeforeUpdate);
	// 	assertThat(issue.getStartedAt()).isBefore(timeBeforeUpdate.plusMinutes(1));
	// }
	//
	// @Test
	// @DisplayName("이슈 상태를 DONE으로 업데이트하는 경우 finishedAt이 현재 날짜와 시간으로 기록된다")
	// void updateIssueStatus_toDone_finishedAtIsRecorded() {
	// 	// given
	// 	CreateIssueRequest createRequest = new CreateIssueRequest(
	// 		IssueType.TASK,
	// 		"Test Issue",
	// 		"Test issue content",
	// 		IssuePriority.HIGH,
	// 		LocalDate.now(),
	// 		null
	// 	);
	// 	issueCommandService.createIssue(workspace.getCode(), createRequest);
	//
	// 	UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.DONE);
	//
	// 	// when
	// 	LocalDateTime timeBeforeUpdate = LocalDateTime.now();
	// 	issueCommandService.updateIssueStatus(1L, workspace.getCode(), updateStatusRequest);
	//
	// 	// then
	// 	Issue issue = issueRepository.findById(1L).orElseThrow();
	//
	// 	assertThat(issue.getFinishedAt()).isAfter(timeBeforeUpdate);
	// 	assertThat(issue.getFinishedAt()).isBefore(timeBeforeUpdate.plusMinutes(1));
	// }
}
