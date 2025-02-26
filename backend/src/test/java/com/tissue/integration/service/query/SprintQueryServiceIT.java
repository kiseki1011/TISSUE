package com.tissue.integration.service.query;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.issue.domain.types.Task;
import com.tissue.api.member.domain.Member;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.presentation.condition.SprintIssueSearchCondition;
import com.tissue.api.sprint.presentation.dto.response.SprintDetailResponse;
import com.tissue.api.sprint.presentation.dto.response.SprintIssueDetail;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SprintQueryServiceIT extends ServiceIntegrationTestHelper {

	Workspace workspace;
	WorkspaceMember owner;
	WorkspaceMember workspaceMember1;
	WorkspaceMember workspaceMember2;
	Story issue1;
	Story issue2;
	Task issue3;

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

		// create issues
		issue1 = testDataFixture.createStory(
			workspace,
			"issue 1",
			IssuePriority.MEDIUM,
			null
		);

		issue2 = testDataFixture.createStory(
			workspace,
			"issue 2",
			IssuePriority.MEDIUM,
			null
		);

		issue3 = testDataFixture.createTask(
			workspace,
			"issue 3",
			IssuePriority.MEDIUM,
			null
		);
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("유효한 스프린트 키와 워크스페이스 코드로 스프린트에 대한 상세 정보를 조회할 수 있다")
	void canGetSprintDetailWithValidSprintKeyAndWorkspaceCode() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build()
		);

		// when
		SprintDetailResponse response = sprintQueryService.getSprintDetail(workspace.getCode(), sprint.getSprintKey());

		// then
		assertThat(response.sprintKey()).isEqualTo(sprint.getSprintKey());
		assertThat(response.title()).isEqualTo(sprint.getTitle());
		assertThat(response.goal()).isEqualTo(sprint.getGoal());
		assertThat(response.issueKeys()).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("스프린트 상세 정보를 조회하는 경우, 등록된 이슈들에 대응되는 이슈 키의 목록을 확인할 수 있다")
	void canGetListOfIssueKeysOfIssuesAddedToSprint_WhenGetSprintDetail() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build()
		);

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);

		// when
		SprintDetailResponse response = sprintQueryService.getSprintDetail(workspace.getCode(), sprint.getSprintKey());

		// then
		assertThat(response.issueKeys()).contains(issue1.getIssueKey(), issue2.getIssueKey());
	}

	@Test
	@Transactional
	@DisplayName("스프린트에 등록된 이슈들을 페이징으로 조회할 수 있다(기본 조건 조회)")
	void getSprintIssues_DefaultCondition() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build()
		);

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);
		sprint.addIssue(issue3);

		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
		SprintIssueSearchCondition condition = new SprintIssueSearchCondition(); // 기본 조건 (TODO, IN_PROGRESS)

		// when
		Page<SprintIssueDetail> result = sprintQueryService.getSprintIssues(
			workspace.getCode(),
			sprint.getSprintKey(),
			condition,
			pageable
		);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(3); // total elements of paging result
		assertThat(result.getContent().size()).isEqualTo(3); // content size of current page, cannot exceed page size
		assertThat(result.getNumber()).isEqualTo(0); // current page
		assertThat(result.getTotalPages()).isEqualTo(1);
	}

	@Test
	@Transactional
	@DisplayName("스프린트에 등록된 이슈들을 이슈 타입(IssueType)으로 필터링 해서 조회할 수 있다(TASK 타입으로 필터링)")
	void getSprintIssues_WithIssueTypeFilter_Task() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build()
		);

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);
		sprint.addIssue(issue3);

		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
		SprintIssueSearchCondition condition = SprintIssueSearchCondition.builder()
			.types(List.of(IssueType.TASK))
			.build();

		// when
		Page<SprintIssueDetail> result = sprintQueryService.getSprintIssues(
			workspace.getCode(),
			sprint.getSprintKey(),
			condition,
			pageable
		);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().size()).isEqualTo(1);
		assertThat(result.getNumber()).isEqualTo(0);
		assertThat(result.getTotalPages()).isEqualTo(1);
	}

	@Test
	@Transactional
	@DisplayName("스프린트에 등록된 이슈들을 이슈 상태(IssueStatus)로 필터링 해서 조회할 수 있다(IN_PROGRESS 타입으로 필터링)")
	void getSprintIssues_WithIssueStatusFilter_InProgress() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build()
		);

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);
		sprint.addIssue(issue3);

		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
		SprintIssueSearchCondition condition = SprintIssueSearchCondition.builder()
			.statuses(List.of(IssueStatus.IN_PROGRESS))
			.build();

		// when
		Page<SprintIssueDetail> result = sprintQueryService.getSprintIssues(
			workspace.getCode(),
			sprint.getSprintKey(),
			condition,
			pageable
		);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(0);
		assertThat(result.getContent().size()).isEqualTo(0);
		assertThat(result.getNumber()).isEqualTo(0);
		assertThat(result.getTotalPages()).isEqualTo(0);
	}

	/**
	 * Can search Issue with title, content, issue key
	 */
	@Test
	@Transactional
	@DisplayName("스프린트에 등록된 이슈들을 특정 검색어로 조회할 수 있다(keyword: 'ISSUE-1')")
	void getSprintIssues_SearchWithKeyword_SearchIssueKey() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build()
		);

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);
		sprint.addIssue(issue3);

		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
		SprintIssueSearchCondition condition = SprintIssueSearchCondition.builder()
			.keyword("ISSUE-1") // search with keyword "ISSUE-1"
			.build();

		// when
		Page<SprintIssueDetail> result = sprintQueryService.getSprintIssues(
			workspace.getCode(),
			sprint.getSprintKey(),
			condition,
			pageable
		);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().size()).isEqualTo(1);
		assertThat(result.getNumber()).isEqualTo(0);
		assertThat(result.getTotalPages()).isEqualTo(1);
	}

	@Test
	@Transactional
	@DisplayName("스프린트에 등록된 이슈들을 특정 검색어로 조회할 수 있다(keyword: 'issue')")
	void getSprintIssues_SearchWithKeyword_SearchTitle() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build()
		);

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);
		sprint.addIssue(issue3);

		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
		SprintIssueSearchCondition condition = SprintIssueSearchCondition.builder()
			.keyword("issue") // search with keyword "issue"
			.build();

		// when
		Page<SprintIssueDetail> result = sprintQueryService.getSprintIssues(
			workspace.getCode(),
			sprint.getSprintKey(),
			condition,
			pageable
		);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(3);
		assertThat(result.getContent().size()).isEqualTo(3);
		assertThat(result.getNumber()).isEqualTo(0);
		assertThat(result.getTotalPages()).isEqualTo(1);
	}
}
