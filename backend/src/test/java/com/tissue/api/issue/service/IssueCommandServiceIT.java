package com.tissue.api.issue.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Epic;
import com.tissue.api.issue.domain.types.Task;
import com.tissue.api.issue.exception.IssueTypeMismatchException;
import com.tissue.api.issue.exception.ParentMustBeEpicException;
import com.tissue.api.issue.exception.SubTaskWrongParentTypeException;
import com.tissue.api.issue.presentation.dto.request.create.CreateStoryRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateSubTaskRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateTaskRequest;
import com.tissue.api.issue.presentation.dto.request.update.UpdateStoryRequest;
import com.tissue.api.issue.presentation.dto.response.create.CreateStoryResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateTaskResponse;
import com.tissue.api.issue.presentation.dto.response.update.UpdateStoryResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.helper.ServiceIntegrationTestHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class IssueCommandServiceIT extends ServiceIntegrationTestHelper {

	@BeforeEach
	void setUp() {
		// 테스트용 워크스페이스 생성
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"Test workspace",
			"This is a test workspace.",
			"TESTCODE",
			null
		);
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Transactional
	@Test
	@DisplayName("TASK 타입 이슈 생성에 성공하면 CreateTaskResponse를 반환 받는다")
	void createTask_Success_returnsCreateTaskResponse() {
		// given
		CreateTaskRequest request = CreateTaskRequest.builder()
			.title("Test Issue")
			.content("Test Content")
			.summary("Test Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.build();

		// when
		CreateTaskResponse response = (CreateTaskResponse)issueCommandService.createIssue(
			"TESTCODE",
			request
		);

		// then
		assertThat(response.getType()).isEqualTo(IssueType.TASK);
		assertThat(response.title()).isEqualTo("Test Issue");

		Issue savedIssue = issueRepository.findById(response.issueId()).orElseThrow();
		assertThat(savedIssue.getWorkspace().getCode()).isEqualTo("TESTCODE");
	}

	@Transactional
	@Test
	@DisplayName("특정 이슈를 부모로 지정하여 이슈를 생성하는 것이 가능하다")
	void createIssue_WithParent() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE")
			.orElseThrow();

		Issue parentIssue = Epic.builder()
			.workspace(workspace)
			.title("Parent Epic Issue")
			.content("Parent Epic Issue")
			.businessGoal("Parent Epic Issue")
			.build();
		issueRepository.save(parentIssue);

		CreateStoryRequest request = CreateStoryRequest.builder()
			.title("Child Story Title")
			.content("Child Story Content")
			.summary("Child Story Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.parentIssueId(parentIssue.getId())
			.userStory("Child Story User Story")
			.acceptanceCriteria("Child Story Acceptance Criteria")
			.build();

		// when
		CreateStoryResponse response = (CreateStoryResponse)issueCommandService.createIssue(
			"TESTCODE",
			request
		);

		// then
		Issue savedIssue = issueRepository.findById(response.issueId()).orElseThrow();

		assertThat(savedIssue.getParentIssue().getId()).isEqualTo(parentIssue.getId());
	}

	@Transactional
	@Test
	@DisplayName("STORY 타입 이슈 생성 시, TASK 타입 이슈를 부모로 지정하면 예외가 발생한다")
	void createStoryIssue_WithParent_Fails_ifParentIsTask() {
		// given
		CreateTaskRequest parentCreateRequest = CreateTaskRequest.builder()
			.title("Parent Task Title")
			.content("Parent Task Content")
			.summary("Parent Task Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.build();

		CreateTaskResponse response = (CreateTaskResponse)issueCommandService.createIssue(
			"TESTCODE",
			parentCreateRequest
		);

		CreateStoryRequest request = CreateStoryRequest.builder()
			.title("Child Story Title")
			.content("Child Story Content")
			.summary("Child Story Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.parentIssueId(response.issueId())
			.userStory("Child Story User Story")
			.acceptanceCriteria("Child Story Acceptance Criteria")
			.build();

		// when & then
		assertThatThrownBy(() -> issueCommandService.createIssue("TESTCODE", request))
			.isInstanceOf(ParentMustBeEpicException.class);
	}

	@Transactional
	@Test
	@DisplayName("이슈 생성 시, EPIC 타입 이슈를 SUB_TASK 타입의 부모로 지정하면 예외가 발생한다")
	void createIssue_WithParent_Fails_ifParentIsEpic_whenChildIsSubTask() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE")
			.orElseThrow();

		Issue parentIssue = Epic.builder()
			.workspace(workspace)
			.title("Parent Epic Title")
			.content("Parent Epic Content")
			.businessGoal("Parent Epic Business Goal")
			.build();
		issueRepository.save(parentIssue);

		CreateSubTaskRequest request = CreateSubTaskRequest.builder()
			.title("Child SubTask Title")
			.content("Child SubTask Content")
			.summary("Child SubTask Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.parentIssueId(parentIssue.getId())
			.build();

		// when & then
		assertThatThrownBy(() -> issueCommandService.createIssue("TESTCODE", request))
			.isInstanceOf(SubTaskWrongParentTypeException.class);
	}

	@Transactional
	@Test
	@DisplayName("이슈 생성 시, 같은 위계의 타입을 가진 이슈를 부모로 지정하면 예외가 발생한다")
	void createIssue_WithParent_Fails_ifParentIsSameHierarchy() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE")
			.orElseThrow();

		Issue parentIssue = Task.builder()
			.workspace(workspace)
			.title("Parent Task Title")
			.content("Parent Task Content")
			.build();
		issueRepository.save(parentIssue);

		CreateTaskRequest request = CreateTaskRequest.builder()
			.title("Child Task Title")
			.content("Child Task Content")
			.summary("Child Task Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.parentIssueId(parentIssue.getId())
			.build();

		// when & then
		assertThatThrownBy(() -> issueCommandService.createIssue("TESTCODE", request))
			.isInstanceOf(ParentMustBeEpicException.class);
	}

	@Transactional
	@Test
	@DisplayName("가장 처음 생성된 이슈의 IssueKey는 'ISSUE-1'이어야 한다")
	void createIssue_firstIssue_issueKeyMustBe_ISSUE_1() {
		// given
		CreateTaskRequest request = CreateTaskRequest.builder()
			.title("Test Task Title")
			.content("Test Task Content")
			.summary("Test Task Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.build();

		// when
		CreateTaskResponse response = (CreateTaskResponse)issueCommandService.createIssue(
			"TESTCODE",
			request
		);

		// then
		assertThat(response.getType()).isEqualTo(IssueType.TASK);
		assertThat(response.title()).isEqualTo("Test Task Title");

		Issue savedIssue = issueRepository.findById(response.issueId()).orElseThrow();
		assertThat(savedIssue.getWorkspace().getCode()).isEqualTo("TESTCODE");
		assertThat(savedIssue.getIssueKey()).isEqualTo("ISSUE-1");
	}

	@Transactional
	@Test
	@DisplayName("두번째 생성된 이슈의 IssueKey는 'ISSUE-2'이어야 한다")
	void createIssue_secondIssue_issueKeyMustBe_ISSUE_2() {
		// given
		CreateTaskRequest request1 = CreateTaskRequest.builder()
			.title("Test Task Title")
			.content("Test Task Content")
			.summary("Test Task Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.build();

		CreateTaskRequest request2 = CreateTaskRequest.builder()
			.title("Second Test Task Title")
			.content("Second Test Task Content")
			.summary("Second Test Task Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.build();

		issueCommandService.createIssue(
			"TESTCODE",
			request1
		);

		// when
		CreateTaskResponse response = (CreateTaskResponse)issueCommandService.createIssue(
			"TESTCODE",
			request2
		);

		// then
		assertThat(response.getType()).isEqualTo(IssueType.TASK);
		assertThat(response.title()).isEqualTo("Second Test Task Title");

		Issue savedIssue = issueRepository.findById(response.issueId()).orElseThrow();
		assertThat(savedIssue.getIssueKey()).isEqualTo("ISSUE-2");
	}

	@Test
	@DisplayName("STORY 타입 이슈 업데이트에 성공하면 UpdateStoryResponse를 반환 받는다")
	void updateIssue_Story_Success() {
		// given
		CreateStoryRequest createStoryRequest = CreateStoryRequest.builder()
			.title("Test Title")
			.content("Test Content")
			.summary("Test Summary")
			.priority(IssuePriority.MEDIUM)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.userStory("Test User Story")
			.acceptanceCriteria("Test Acceptance Criteria")
			.build();

		CreateStoryResponse createResponse = (CreateStoryResponse)issueCommandService.createIssue(
			"TESTCODE",
			createStoryRequest
		);

		UpdateStoryRequest request = UpdateStoryRequest.builder()
			.title("Updated Title")
			.content("Updated Content")
			.summary("Updated Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.HARD)
			.userStory("Updated User Story")
			.acceptanceCriteria("Updated Acceptance Criteria")
			.build();

		// when
		UpdateStoryResponse response = (UpdateStoryResponse)issueCommandService.updateIssue(
			"TESTCODE",
			createResponse.issueKey(),
			request
		);

		// then
		assertThat(response.issueKey()).isEqualTo(createResponse.issueKey());
		assertThat(response.title()).isEqualTo("Updated Title");
	}

	@Test
	void updateIssue_TypeMismatch_ThrowsException() {
		// given
		CreateTaskRequest createTaskRequest = CreateTaskRequest.builder()
			.title("Test Task Title")
			.content("Test Task Content")
			.summary("Test Task Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.NORMAL)
			.build();

		CreateTaskResponse createResponse = (CreateTaskResponse)issueCommandService.createIssue(
			"TESTCODE",
			createTaskRequest
		);

		UpdateStoryRequest request = UpdateStoryRequest.builder()
			.title("Updated Title")
			.content("Updated Content")
			.summary("Updated Summary")
			.priority(IssuePriority.HIGH)
			.dueDate(LocalDate.now())
			.difficulty(Difficulty.HARD)
			.userStory("Updated User Story")
			.acceptanceCriteria("Updated Acceptance Criteria")
			.build();

		// when & then
		assertThatThrownBy(() -> issueCommandService.updateIssue("TESTCODE", createResponse.issueKey(), request))
			.isInstanceOf(IssueTypeMismatchException.class);
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
	// 	issueCommandService.createIssue("TESTCODE", createRequest);
	//
	// 	UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.IN_PROGRESS);
	//
	// 	// when
	// 	UpdateStatusResponse response = issueCommandService.updateIssueStatus(1L, "TESTCODE", updateStatusRequest);
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
	// 	issueCommandService.createIssue("TESTCODE", createRequest);
	//
	// 	UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.IN_REVIEW);
	//
	// 	// when & then
	// 	assertThatThrownBy(() -> issueCommandService.updateIssueStatus(1L, "TESTCODE", updateStatusRequest))
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
	// 	issueCommandService.createIssue("TESTCODE", createRequest);
	//
	// 	UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.IN_PROGRESS);
	//
	// 	// when
	// 	LocalDateTime timeBeforeUpdate = LocalDateTime.now();
	// 	issueCommandService.updateIssueStatus(1L, "TESTCODE", updateStatusRequest);
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
	// 	issueCommandService.createIssue("TESTCODE", createRequest);
	//
	// 	UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.DONE);
	//
	// 	// when
	// 	LocalDateTime timeBeforeUpdate = LocalDateTime.now();
	// 	issueCommandService.updateIssueStatus(1L, "TESTCODE", updateStatusRequest);
	//
	// 	// then
	// 	Issue issue = issueRepository.findById(1L).orElseThrow();
	//
	// 	assertThat(issue.getFinishedAt()).isAfter(timeBeforeUpdate);
	// 	assertThat(issue.getFinishedAt()).isBefore(timeBeforeUpdate.plusMinutes(1));
	// }
}
