package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.base.domain.enums.IssuePriority;
import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.member.domain.model.Member;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.domain.model.SprintIssue;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;
import com.tissue.api.sprint.presentation.dto.request.AddSprintIssuesRequest;
import com.tissue.api.sprint.presentation.dto.request.CreateSprintRequest;
import com.tissue.api.sprint.presentation.dto.request.RemoveSprintIssueRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintStatusRequest;
import com.tissue.api.sprint.presentation.dto.response.SprintResponse;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

public class SprintCommandServiceIT extends ServiceIntegrationTestHelper {

	Workspace workspace;
	WorkspaceMember owner;
	WorkspaceMember workspaceMember1;
	WorkspaceMember workspaceMember2;
	Story issue1;
	Story issue2;

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
	}

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("스프린트를 생성할 수 있다")
	void canCreateSprintWithValidWorkspaceCode() {
		// given
		CreateSprintRequest request = CreateSprintRequest.builder()
			.title("test sprint")
			.goal("test sprint")
			.plannedStartDate(LocalDateTime.now())
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.build();

		// when
		SprintResponse response = sprintCommandService.createSprint(workspace.getKey(), request);

		// then
		assertThat(response.workspaceCode()).isEqualTo(workspace.getKey());

		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(
			response.sprintKey(),
			response.workspaceCode()
		).get();

		assertThat(foundSprint.getTitle()).isEqualTo("test sprint");
	}

	@Test
	@DisplayName("스프린트를 생성하면 스프린트 키(sprintKey)의 접두사는 'SPRINT'로 설정된다")
	void prefixOfSprintKeyIsSPRINT() {
		// given
		CreateSprintRequest request = CreateSprintRequest.builder()
			.title("test sprint")
			.goal("test sprint")
			.plannedStartDate(LocalDateTime.now())
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.build();

		// when
		SprintResponse response = sprintCommandService.createSprint(workspace.getKey(), request);

		// then
		assertThat(response.sprintKey()).isEqualTo("SPRINT-1");
	}

	@Test
	@DisplayName("스프린트를 생성하면 스프린트 키(sprintKey)의 번호가 1씩 증가한다")
	void whenCreatingSprint_SprintKeyNumberIncreasesByOne() {
		// given
		SprintResponse firstSprintResponse = sprintCommandService.createSprint(
			workspace.getKey(),
			CreateSprintRequest.builder()
				.title("first sprint")
				.goal("first sprint")
				.plannedStartDate(LocalDateTime.now())
				.plannedEndDate(LocalDateTime.now().plusDays(1))
				.build()
		);

		CreateSprintRequest request = CreateSprintRequest.builder()
			.title("second sprint")
			.goal("second sprint")
			.plannedStartDate(LocalDateTime.now())
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.build();

		// when
		SprintResponse secondSprintResponse = sprintCommandService.createSprint(workspace.getKey(), request);

		// then
		assertThat(firstSprintResponse.sprintKey()).isEqualTo("SPRINT-1");
		assertThat(secondSprintResponse.sprintKey()).isEqualTo("SPRINT-2");
	}

	@Test
	@Transactional
	@DisplayName("여러개의 이슈를 한번에 스프린트에 등록할 수 있다")
	void canAddMultipleIssuesAsBulkToSprint() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		AddSprintIssuesRequest request = new AddSprintIssuesRequest(
			List.of(issue1.getIssueKey(), issue2.getIssueKey())
		);

		// when
		SprintResponse response = sprintCommandService.addIssues(
			workspace.getKey(),
			sprint.getKey(),
			request
		);

		// then
		assertThat(response.sprintKey()).isEqualTo(sprint.getKey());

		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(
			response.sprintKey(),
			response.workspaceCode()
		).get();

		assertThat(foundSprint.getSprintIssues().stream().map(SprintIssue::getIssue).map(Issue::getKey).toList())
			.isEqualTo(List.of(issue1.getIssueKey(), issue2.getIssueKey()));
	}

	@Test
	@DisplayName("이슈를 스프린트에 추가하는 경우, 이슈 키 중 하나라도 유효하지 않다면 실패한다")
	void addingIssuesToSprintFails_IfInvalidIssueKeyExists() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		AddSprintIssuesRequest request = new AddSprintIssuesRequest(
			List.of(issue1.getIssueKey(), issue2.getIssueKey(), "INVALID-KEY")
		);

		// when & then
		assertThatThrownBy(() -> sprintCommandService.addIssues(
				workspace.getKey(),
				sprint.getKey(),
				request
			)
		).isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	@Transactional
	@DisplayName("스프린트에 등록된 이슈를 해제할 수 있다")
	void canRemoveIssueFromSprint() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);

		// when
		sprintCommandService.removeIssue(
			workspace.getKey(),
			sprint.getKey(),
			new RemoveSprintIssueRequest(issue1.getIssueKey())
		);

		// then
		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(
				sprint.getKey(),
				workspace.getKey())
			.get();

		assertThat(foundSprint.getSprintIssues().get(0).getIssue()).isEqualTo(issue2);
	}

	@Test
	@DisplayName("스프린트의 내용(title, goal)을 업데이트할 수 있다")
	void canUpdateSprintContent() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		UpdateSprintRequest request = UpdateSprintRequest.builder()
			.title("updated title")
			.goal("updated goal")
			.build();

		// when
		SprintResponse response = sprintCommandService.updateSprint(
			workspace.getKey(),
			sprint.getKey(),
			request
		);

		// then
		assertThat(response.sprintKey()).isEqualTo(sprint.getKey());

		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(
			response.sprintKey(),
			response.workspaceCode()
		).get();

		assertThat(foundSprint.getTitle()).isEqualTo("updated title");
		assertThat(foundSprint.getGoal()).isEqualTo("updated goal");
	}

	@Test
	@DisplayName("스프린트의 내용 업데이트 시, 요청에서 값을 명시한 필드만 업데이트된다(만약 goal의 값이 없으면 업데이트 대상이 아니다)")
	void onlyRequestFieldsNotNullAreUpdated() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		// leave out value of goal
		UpdateSprintRequest request = UpdateSprintRequest.builder()
			.title("updated title")
			.build();

		// when
		SprintResponse response = sprintCommandService.updateSprint(
			workspace.getKey(),
			sprint.getKey(),
			request
		);

		// then
		assertThat(response.sprintKey()).isEqualTo(sprint.getKey());

		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(
			response.sprintKey(),
			response.workspaceCode()
		).get();

		assertThat(foundSprint.getTitle()).isEqualTo("updated title");
		assertThat(foundSprint.getGoal()).isEqualTo("original goal");
	}

	@Test
	@DisplayName("스프린트의 계획된 시작일자, 종료일자(plannedStartDate, plannedEndDate)를 업데이트할 수 있다")
	void canUpdateSprintDates() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		UpdateSprintRequest request = UpdateSprintRequest.builder()
			.plannedStartDate(LocalDateTime.now().plusDays(7))
			.plannedEndDate(LocalDateTime.now().plusDays(14))
			.build();

		// when
		SprintResponse response = sprintCommandService.updateSprint(
			workspace.getKey(),
			sprint.getKey(),
			request
		);

		// then
		assertThat(response.sprintKey()).isEqualTo(sprint.getKey());

		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(
			response.sprintKey(),
			response.workspaceCode()
		).get();

		assertThat(foundSprint.getPlannedStartDate().toLocalDate()).isEqualTo(LocalDate.now().plusDays(7));
		assertThat(foundSprint.getPlannedEndDate().toLocalDate()).isEqualTo(LocalDate.now().plusDays(14));
	}

	@Test
	@DisplayName("스프린트의 종료일자를 시작일자 이전으로 업데이트할 수 없다")
	void cannotUpdateSprintEndDateToBeBeforeStartDate() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		// endDate is 7 days before startDate
		UpdateSprintRequest request = UpdateSprintRequest.builder()
			.plannedStartDate(LocalDateTime.now().plusDays(14))
			.plannedEndDate(LocalDateTime.now().plusDays(7))
			.build();

		// when & then
		assertThatThrownBy(() -> sprintCommandService.updateSprint(
				workspace.getKey(),
				sprint.getKey(),
				request
			)
		).isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("PLANNING에서 ACTIVE로 스프린트의 상태를 변경할 수 있다(스프린트를 시작할 수 있다)")
	void canChangeStatusOfSprintFromPlanningToActive() {
		// given
		Long currentWorkspaceMemberId = workspaceMember1.getId();

		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		// when
		SprintResponse response = sprintCommandService.updateSprintStatus(
			workspace.getKey(),
			sprint.getKey(),
			new UpdateSprintStatusRequest(SprintStatus.ACTIVE),
			currentWorkspaceMemberId
		);

		// then
		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(
			response.sprintKey(),
			response.workspaceCode()
		).get();

		assertThat(foundSprint.getStatus()).isEqualTo(SprintStatus.ACTIVE);
	}

	@Test
	@DisplayName("PLANNING에서 CANCELLED로 스프린트의 상태를 변경할 수 있다(스프린트를 취소할 수 있다)")
	void canChangeStatusOfSprintFromPlanningToCancelled() {
		// given
		Long currentWorkspaceMemberId = workspaceMember1.getId();

		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		// when
		SprintResponse response = sprintCommandService.updateSprintStatus(
			workspace.getKey(),
			sprint.getKey(),
			new UpdateSprintStatusRequest(SprintStatus.CANCELLED),
			currentWorkspaceMemberId
		);

		// then
		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(
			response.sprintKey(),
			response.workspaceCode()
		).get();

		assertThat(foundSprint.getStatus()).isEqualTo(SprintStatus.CANCELLED);
	}

	@Test
	@DisplayName("현재 날짜가 종료일자를 지났으면, PLANNING에서 ACTIVE로 스프린트의 상태를 변경할 수 없다(스프린트를 시작할 수 없다)")
	void cannotChangeStatusOfSprintFromPlanningToActive_IfEndDateHasPassed() {
		// given - sprint ended yesterday
		Long currentWorkspaceMemberId = workspaceMember1.getId();

		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.plannedStartDate(LocalDateTime.now().minusDays(2))
			.plannedEndDate(LocalDateTime.now().minusDays(1))
			.workspace(workspace)
			.build());

		// when & then
		assertThatThrownBy(() -> sprintCommandService.updateSprintStatus(
				workspace.getKey(),
				sprint.getKey(),
				new UpdateSprintStatusRequest(SprintStatus.ACTIVE),
				currentWorkspaceMemberId
			)
		).isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("이미 설정되어 있던 상태로 스프린트의 상태를 변경할 수 없다")
	void cannotChangeStatusToSameStatus() {
		// given
		Long currentWorkspaceMemberId = workspaceMember1.getId();

		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		// when & then
		assertThatThrownBy(() -> sprintCommandService.updateSprintStatus(
				workspace.getKey(),
				sprint.getKey(),
				new UpdateSprintStatusRequest(SprintStatus.PLANNING),
				currentWorkspaceMemberId
			)
		).isInstanceOf(InvalidOperationException.class);
	}

	@ParameterizedTest
	@EnumSource(value = SprintStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("ACTIVE에서 COMPLETED 또는 CANCELLED로 스프린트의 상태를 변경할 수 있다(진행되고 있는 스프린트를 완료하거나 취소할 수 있다)")
	void canCompleteOrCancel_ActiveSprint(SprintStatus status) {
		// given
		Long currentWorkspaceMemberId = workspaceMember1.getId();

		Sprint sprint = Sprint.builder()
			.title("original title")
			.goal("original goal")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build();
		sprint.updateStatus(SprintStatus.ACTIVE);
		sprintRepository.save(sprint);

		// when
		SprintResponse response = sprintCommandService.updateSprintStatus(
			workspace.getKey(),
			sprint.getKey(),
			new UpdateSprintStatusRequest(status),
			currentWorkspaceMemberId
		);

		// then
		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(
			response.sprintKey(),
			response.workspaceCode()
		).get();

		assertThat(foundSprint.getStatus()).isEqualTo(status);
	}

	@ParameterizedTest
	@EnumSource(value = SprintStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("COMPLETED 또는 CANCELLED된 스프린트의 상태를 변경할 수 없다(완료되거나 취소된 스프린트의 상태를 변경할 수 없다)")
	void cannotChangeCompletedOrCancelledSprintStatus(SprintStatus status) {
		// given
		Long currentWorkspaceMemberId = workspaceMember1.getId();

		Sprint sprint = Sprint.builder()
			.title("original title")
			.goal("original goal")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build();
		sprint.updateStatus(SprintStatus.ACTIVE);
		sprint.updateStatus(status);
		sprintRepository.save(sprint);

		// when & then
		assertThatThrownBy(() -> sprintCommandService.updateSprintStatus(
			workspace.getKey(),
			sprint.getKey(),
			new UpdateSprintStatusRequest(SprintStatus.PLANNING),
			currentWorkspaceMemberId
		)).isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("워크스페이스 내에서는 동시에 하나의 스프린트만 ACTIVE로 존재할 수 있다(동시에 하나의 스프린트만 진행 가능)")
	void onlyASingleSprintCanBeActiveSimultaneously() {
		// given
		Long currentWorkspaceMemberId = workspaceMember1.getId();

		Sprint sprint1 = Sprint.builder()
			.title("sprint 1")
			.goal("sprint 1")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build();

		sprint1.updateStatus(SprintStatus.ACTIVE);
		sprintRepository.save(sprint1);

		Sprint sprint2 = sprintRepository.save(Sprint.builder()
			.title("sprint 2")
			.goal("sprint 2")
			.plannedStartDate(LocalDateTime.now().plusDays(2))
			.plannedEndDate(LocalDateTime.now().plusDays(3))
			.workspace(workspace)
			.build());

		// when & then
		assertThatThrownBy(() -> sprintCommandService.updateSprintStatus(
				workspace.getKey(),
				sprint2.getKey(),
				new UpdateSprintStatusRequest(SprintStatus.ACTIVE),
				currentWorkspaceMemberId
			)
		).isInstanceOf(InvalidOperationException.class);
	}

	@ParameterizedTest
	@Transactional
	@EnumSource(value = SprintStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("스프린트 상태가 COMPLETED이거나 CANCELLED인 경우, 스프린트에 등록된 이슈를 해제할 수 없다")
	void cannotRemoveIssueFromSprint_IfSprintIsCompletedOrCancelled(SprintStatus status) {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.plannedStartDate(LocalDateTime.now().minusDays(1))
			.plannedEndDate(LocalDateTime.now().plusDays(1))
			.workspace(workspace)
			.build());

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);

		sprint.updateStatus(SprintStatus.ACTIVE);
		sprint.updateStatus(status);

		// when & then
		assertThatThrownBy(() -> sprintCommandService.removeIssue(
				workspace.getKey(),
				sprint.getKey(),
				new RemoveSprintIssueRequest(issue1.getIssueKey())
			)
		).isInstanceOf(InvalidOperationException.class);
	}
}
