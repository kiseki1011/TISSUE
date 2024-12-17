package com.uranus.taskmanager.api.issue.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.IssueType;
import com.uranus.taskmanager.api.issue.presentation.dto.request.CreateIssueRequest;
import com.uranus.taskmanager.api.issue.presentation.dto.response.CreateIssueResponse;
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
			.isInstanceOf(IllegalArgumentException.class); // Todo: 커스텀 예외 만들면 수정
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
			.isInstanceOf(IllegalArgumentException.class); // Todo: 커스텀 예외 만들면 수정
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
			.isInstanceOf(IllegalArgumentException.class); // Todo: 커스텀 예외 만들면 수정
	}

}