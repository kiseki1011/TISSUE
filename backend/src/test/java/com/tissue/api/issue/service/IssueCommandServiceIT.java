package com.tissue.api.issue.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.assertj.core.api.Assertions;
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
import com.tissue.api.issue.exception.ParentMustBeEpicException;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateStoryRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateSubTaskRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateTaskRequest;
import com.tissue.api.issue.presentation.dto.response.create.CreateStoryResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateTaskResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.helper.ServiceIntegrationTestHelper;
import com.tissue.api.issue.exception.SubTaskWrongParentTypeException;

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
	@DisplayName("이슈 생성에 성공하면 이슈 생성 응답을 반환 받는다")
	void createIssue_Success_returnsCreateIssueResponse() {
		// given
		CreateIssueRequest request = new CreateTaskRequest(
			"Test Issue",
			"Test content",
			null,
			IssuePriority.HIGH,
			LocalDate.now(),
			Difficulty.NORMAL,
			null
		);

		// when
		CreateTaskResponse response = (CreateTaskResponse)issueCommandService.createIssue(
			"TESTCODE",
			request
		);

		// then
		log.info("response = {}", response);

		Assertions.assertThat(response.getType()).isEqualTo(IssueType.TASK);
		assertThat(response.title()).isEqualTo("Test Issue");

		Issue savedIssue = issueRepository.findById(response.issueId()).orElseThrow();
		Assertions.assertThat(savedIssue.getWorkspace().getCode()).isEqualTo("TESTCODE");
	}

	@Transactional
	@Test
	@DisplayName("부모 이슈를 지정하여 이슈를 생성할 수 있다")
	void createIssue_WithParent_Success() {
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

		CreateIssueRequest request = new CreateStoryRequest(
			"Child Story Issue",
			"Child Story Issue",
			null,
			IssuePriority.HIGH,
			LocalDate.now(),
			Difficulty.NORMAL,
			parentIssue.getId(),
			"Child Story Issue User Story",
			"Child Story Issue Acceptance Criteria"
		);

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
		CreateTaskRequest parentCreateRequest = new CreateTaskRequest(
			"Parent Task Issue",
			"Parent Task Issue",
			"Parent Task Issue",
			null,
			LocalDate.now(),
			Difficulty.NORMAL,
			null
		);

		CreateTaskResponse response = (CreateTaskResponse)issueCommandService.createIssue(
			"TESTCODE",
			parentCreateRequest
		);

		Long parentIssueId = response.issueId();

		CreateStoryRequest request = new CreateStoryRequest(
			"Child Issue",
			"Child issue content",
			"Test summary",
			IssuePriority.HIGH,
			LocalDate.now(),
			Difficulty.NORMAL,
			parentIssueId,
			"Test user story",
			"Acceptance Criteria"
		);

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
			.title("Parent Issue")
			.content("Parent issue content")
			.businessGoal("Test business goal")
			.build();
		issueRepository.save(parentIssue);

		CreateIssueRequest request = new CreateSubTaskRequest(
			"Child Issue",
			"Child issue content",
			null,
			IssuePriority.HIGH,
			LocalDate.now(),
			Difficulty.NORMAL,
			parentIssue.getId()
		);

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
			.title("Parent Issue")
			.content("Parent issue content")
			.build();
		issueRepository.save(parentIssue);

		CreateIssueRequest request = new CreateTaskRequest(
			"Child Issue",
			"Child issue content",
			null,
			IssuePriority.HIGH,
			LocalDate.now(),
			Difficulty.NORMAL,
			parentIssue.getId()
		);

		// when & then
		assertThatThrownBy(() -> issueCommandService.createIssue("TESTCODE", request))
			.isInstanceOf(ParentMustBeEpicException.class);
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
