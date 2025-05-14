package com.tissue.integration.service.query;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;
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

import com.tissue.api.issue.domain.model.enums.IssuePriority;
import com.tissue.api.issue.domain.model.enums.IssueStatus;
import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.issue.domain.model.types.Story;
import com.tissue.api.issue.domain.model.types.Task;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;
import com.tissue.api.sprint.presentation.condition.SprintIssueSearchCondition;
import com.tissue.api.sprint.presentation.condition.SprintSearchCondition;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintStatusRequest;
import com.tissue.api.sprint.presentation.dto.response.SprintDetail;
import com.tissue.api.sprint.presentation.dto.response.SprintIssueDetail;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
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
			LocalDateTime.now().plusDays(7)
		);

		issue2 = testDataFixture.createStory(
			workspace,
			"issue 2",
			IssuePriority.MEDIUM,
			LocalDateTime.now().plusDays(7)
		);

		issue3 = testDataFixture.createTask(
			workspace,
			"issue 3",
			IssuePriority.MEDIUM,
			LocalDateTime.now().plusDays(7)
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
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build()
		);

		// when
		SprintDetail response = sprintQueryService.getSprintDetail(workspace.getCode(), sprint.getSprintKey());

		// then
		assertThat(response.sprintKey()).isEqualTo(sprint.getSprintKey());
		assertThat(response.title()).isEqualTo(sprint.getTitle());
		assertThat(response.goal()).isEqualTo(sprint.getGoal());
		assertThat(response.issueKeys()).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("특정 워크스페이스의 스프린트들을 페이징으로 조회할 수 있다(기본 조건 조회)")
	void getSprints_Page_DefaultCondition() {
		// given
		Long currentWorkspaceMemberId = workspaceMember1.getId();

		Sprint sprint1 = sprintRepository.save(Sprint.builder()
			.title("sprint 1")
			.goal("sprint 1")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build()
		);

		Sprint sprint2 = sprintRepository.save(Sprint.builder()
			.title("sprint 2")
			.goal("sprint 2")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(2))
			.workspace(workspace)
			.build()
		);

		Sprint sprint3 = sprintRepository.save(Sprint.builder()
			.title("sprint 3")
			.goal("sprint 3")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(3))
			.workspace(workspace)
			.build()
		);

		sprintCommandService.updateSprintStatus(
			workspace.getCode(),
			sprint1.getSprintKey(),
			new UpdateSprintStatusRequest(SprintStatus.ACTIVE),
			currentWorkspaceMemberId
		);

		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdDate").descending());
		SprintSearchCondition searchCondition = new SprintSearchCondition(); // default condition

		// when
		Page<SprintDetail> result = sprintQueryService.getSprints(workspace.getCode(), searchCondition, pageable);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getTotalElements()).isEqualTo(3); // total elements of paging result
		assertThat(result.getContent().size()).isEqualTo(3); // content size of current page, cannot exceed page size
		assertThat(result.getNumber()).isEqualTo(0); // current page
		assertThat(result.getTotalPages()).isEqualTo(1);
	}

	@Test
	@Transactional
	@DisplayName("스프린트 상세 정보를 조회하는 경우, 등록된 이슈들에 대응되는 이슈 키의 목록을 확인할 수 있다")
	void canGetListOfIssueKeysOfIssuesAddedToSprint_WhenGetSprintDetail() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build()
		);

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);

		// when
		SprintDetail response = sprintQueryService.getSprintDetail(workspace.getCode(), sprint.getSprintKey());

		// then
		assertThat(response.issueKeys()).contains(issue1.getIssueKey(), issue2.getIssueKey());
	}

	@Test
	@Transactional
	@DisplayName("스프린트에 등록된 이슈들을 페이징으로 조회할 수 있다(기본 조건 조회)")
	void getSprintIssues_Page_DefaultCondition() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
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
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
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
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
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
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
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
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
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
