package com.uranus.taskmanager.api.issue.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.IssueStatus;
import com.uranus.taskmanager.api.issue.domain.IssueType;
import com.uranus.taskmanager.api.issue.exception.DirectUpdateToInReviewException;
import com.uranus.taskmanager.api.issue.exception.SubTaskParentIssueException;
import com.uranus.taskmanager.api.issue.exception.SubTaskWrongParentTypeException;
import com.uranus.taskmanager.api.issue.exception.WrongChildIssueTypeException;
import com.uranus.taskmanager.api.issue.presentation.dto.request.CreateIssueRequest;
import com.uranus.taskmanager.api.issue.presentation.dto.request.UpdateStatusRequest;
import com.uranus.taskmanager.api.issue.presentation.dto.response.CreateIssueResponse;
import com.uranus.taskmanager.api.issue.presentation.dto.response.UpdateStatusResponse;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

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
		CreateIssueRequest request = new CreateIssueRequest(
			IssueType.TASK,
			"Test Issue",
			"Test content",
			IssuePriority.HIGH,
			LocalDate.now(),
			null
		);

		// when
		CreateIssueResponse response = issueCommandService.createIssue(
			"TESTCODE",
			request
		);

		// then
		assertThat(response.title()).isEqualTo("Test Issue");
		assertThat(response.type()).isEqualTo(IssueType.TASK);
		assertThat(response.parentIssueId()).isNull();

		Issue savedIssue = issueRepository.findById(response.issueId())
			.orElseThrow();
		assertThat(savedIssue.getWorkspace().getCode()).isEqualTo("TESTCODE");
	}

	@Transactional
	@Test
	@DisplayName("부모 이슈를 지정하여 이슈를 생성할 수 있다")
	void createIssue_WithParent_Success() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE")
			.orElseThrow();

		Issue parentIssue = Issue.builder()
			.workspace(workspace)
			.type(IssueType.EPIC)
			.title("Parent Issue")
			.content("Parent issue content")
			.build();
		issueRepository.save(parentIssue);

		CreateIssueRequest request = new CreateIssueRequest(
			IssueType.STORY,
			"Child Issue",
			"Child issue content",
			IssuePriority.HIGH,
			LocalDate.now(),
			parentIssue.getId()
		);

		// when
		CreateIssueResponse response = issueCommandService.createIssue(
			"TESTCODE",
			request
		);

		// then
		Issue savedIssue = issueRepository.findById(response.issueId()).orElseThrow();

		assertThat(savedIssue.getParentIssue().getId()).isEqualTo(parentIssue.getId());
	}

	@Transactional
	@Test
	@DisplayName("이슈 생성 시, SUB_TASK 타입 이슈를 부모로 지정하면 예외가 발생한다")
	void createIssue_WithParent_Fails_ifParentIsSubTask() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE")
			.orElseThrow();

		Issue parentIssue = Issue.builder()
			.workspace(workspace)
			.type(IssueType.SUB_TASK)
			.title("Parent Issue")
			.content("Parent issue content")
			.build();
		issueRepository.save(parentIssue);

		CreateIssueRequest request = new CreateIssueRequest(
			IssueType.STORY,
			"Child Issue",
			"Child issue content",
			IssuePriority.HIGH,
			LocalDate.now(),
			parentIssue.getId()
		);

		// when & then
		assertThatThrownBy(() -> issueCommandService.createIssue("TESTCODE", request))
			.isInstanceOf(SubTaskParentIssueException.class);
	}

	@Transactional
	@Test
	@DisplayName("이슈 생성 시, EPIC 타입 이슈를 SUB_TASK 타입의 부모로 지정하면 예외가 발생한다")
	void createIssue_WithParent_Fails_ifParentIsEpic_whenChildIsSubTask() {
		// given
		Workspace workspace = workspaceRepository.findByCode("TESTCODE")
			.orElseThrow();

		Issue parentIssue = Issue.builder()
			.workspace(workspace)
			.type(IssueType.EPIC)
			.title("Parent Issue")
			.content("Parent issue content")
			.build();
		issueRepository.save(parentIssue);

		CreateIssueRequest request = new CreateIssueRequest(
			IssueType.SUB_TASK,
			"Child Issue",
			"Child issue content",
			IssuePriority.HIGH,
			LocalDate.now(),
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

		Issue parentIssue = Issue.builder()
			.workspace(workspace)
			.type(IssueType.TASK)
			.title("Parent Issue")
			.content("Parent issue content")
			.build();
		issueRepository.save(parentIssue);

		CreateIssueRequest request = new CreateIssueRequest(
			IssueType.TASK,
			"Child Issue",
			"Child issue content",
			IssuePriority.HIGH,
			LocalDate.now(),
			parentIssue.getId()
		);

		// when & then
		assertThatThrownBy(() -> issueCommandService.createIssue("TESTCODE", request))
			.isInstanceOf(WrongChildIssueTypeException.class);
	}

	@Test
	@DisplayName("이슈 상태 업데이트를 성공하면 이슈 상태 업데이트 응답을 반환한다")
	void updateIssueStatus_success_returnUpdateStatusResponse() {
		// given
		CreateIssueRequest createRequest = new CreateIssueRequest(
			IssueType.TASK,
			"Test Issue",
			"Test issue content",
			IssuePriority.HIGH,
			LocalDate.now(),
			null
		);
		issueCommandService.createIssue("TESTCODE", createRequest);

		UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.IN_PROGRESS);

		// when
		UpdateStatusResponse response = issueCommandService.updateIssueStatus(1L, "TESTCODE", updateStatusRequest);

		// then
		assertThat(response.issueId()).isEqualTo(1L);
		assertThat(response.status()).isEqualTo(IssueStatus.IN_PROGRESS);
	}

	@Test
	@DisplayName("이슈 상태를 IN_REVIEW로 직접 업데이트 시도하는 경우 예외가 발생한다")
	void updateIssueStatus_fails_ifUpdateDirectlyToInReview() {
		// given
		CreateIssueRequest createRequest = new CreateIssueRequest(
			IssueType.TASK,
			"Test Issue",
			"Test issue content",
			IssuePriority.HIGH,
			LocalDate.now(),
			null
		);
		issueCommandService.createIssue("TESTCODE", createRequest);

		UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.IN_REVIEW);

		// when & then
		assertThatThrownBy(() -> issueCommandService.updateIssueStatus(1L, "TESTCODE", updateStatusRequest))
			.isInstanceOf(DirectUpdateToInReviewException.class);
	}

	@Test
	@DisplayName("이슈 상태를 처음으로 IN_PROGRESS로 업데이트하는 경우, startedAt이 현재 날짜와 시간으로 기록된다")
	void updateIssueStatus_toInProgress_startedAtIsRecorded() {
		// given
		CreateIssueRequest createRequest = new CreateIssueRequest(
			IssueType.TASK,
			"Test Issue",
			"Test issue content",
			IssuePriority.HIGH,
			LocalDate.now(),
			null
		);
		issueCommandService.createIssue("TESTCODE", createRequest);

		UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.IN_PROGRESS);

		// when
		LocalDateTime timeBeforeUpdate = LocalDateTime.now();
		issueCommandService.updateIssueStatus(1L, "TESTCODE", updateStatusRequest);

		// then
		Issue issue = issueRepository.findById(1L).orElseThrow();

		assertThat(issue.getStartedAt()).isAfter(timeBeforeUpdate);
		assertThat(issue.getStartedAt()).isBefore(timeBeforeUpdate.plusMinutes(1));
	}

	@Test
	@DisplayName("이슈 상태를 DONE으로 업데이트하는 경우 finishedAt이 현재 날짜와 시간으로 기록된다")
	void updateIssueStatus_toDone_finishedAtIsRecorded() {
		// given
		CreateIssueRequest createRequest = new CreateIssueRequest(
			IssueType.TASK,
			"Test Issue",
			"Test issue content",
			IssuePriority.HIGH,
			LocalDate.now(),
			null
		);
		issueCommandService.createIssue("TESTCODE", createRequest);

		UpdateStatusRequest updateStatusRequest = new UpdateStatusRequest(IssueStatus.DONE);

		// when
		LocalDateTime timeBeforeUpdate = LocalDateTime.now();
		issueCommandService.updateIssueStatus(1L, "TESTCODE", updateStatusRequest);

		// then
		Issue issue = issueRepository.findById(1L).orElseThrow();

		assertThat(issue.getFinishedAt()).isAfter(timeBeforeUpdate);
		assertThat(issue.getFinishedAt()).isBefore(timeBeforeUpdate.plusMinutes(1));
	}
}
