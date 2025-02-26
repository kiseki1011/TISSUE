package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
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
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.member.domain.Member;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.enums.SprintStatus;
import com.tissue.api.sprint.presentation.dto.request.AddSprintIssuesRequest;
import com.tissue.api.sprint.presentation.dto.request.CreateSprintRequest;
import com.tissue.api.sprint.presentation.dto.request.RemoveSprintIssueRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintContentRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintDateRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintStatusRequest;
import com.tissue.api.sprint.presentation.dto.response.AddSprintIssuesResponse;
import com.tissue.api.sprint.presentation.dto.response.CreateSprintResponse;
import com.tissue.api.sprint.presentation.dto.response.UpdateSprintContentResponse;
import com.tissue.api.sprint.presentation.dto.response.UpdateSprintDateResponse;
import com.tissue.api.sprint.presentation.dto.response.UpdateSprintStatusResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
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
			null
		);

		issue2 = testDataFixture.createStory(
			workspace,
			"issue 2",
			IssuePriority.MEDIUM,
			null
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
			.startDate(LocalDate.now())
			.endDate(LocalDate.now().plusDays(1))
			.build();

		// when
		CreateSprintResponse response = sprintCommandService.createSprint(workspace.getCode(), request);

		// then
		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(response.sprintKey(), workspace.getCode())
			.get();

		assertThat(foundSprint.getTitle()).isEqualTo("test sprint");
		assertThat(response.title()).isEqualTo("test sprint");
	}

	@Test
	@DisplayName("스프린트를 생성하면 스프린트 키(sprintKey)의 접두사는 'SPRINT'로 설정된다")
	void prefixOfSprintKeyIsSPRINT() {
		// given
		CreateSprintRequest request = CreateSprintRequest.builder()
			.title("test sprint")
			.goal("test sprint")
			.startDate(LocalDate.now())
			.endDate(LocalDate.now().plusDays(1))
			.build();

		// when
		CreateSprintResponse response = sprintCommandService.createSprint(workspace.getCode(), request);

		// then
		assertThat(response.sprintKey()).isEqualTo("SPRINT-1");
	}

	@Test
	@DisplayName("스프린트를 생성하면 스프린트 키(sprintKey)의 번호가 1씩 증가한다")
	void whenCreatingSprint_SprintKeyNumberIncreasesByOne() {
		// given
		CreateSprintResponse firstSprintResponse = sprintCommandService.createSprint(
			workspace.getCode(),
			CreateSprintRequest.builder()
				.title("first sprint")
				.goal("first sprint")
				.startDate(LocalDate.now())
				.endDate(LocalDate.now().plusDays(1))
				.build()
		);

		CreateSprintRequest request = CreateSprintRequest.builder()
			.title("second sprint")
			.goal("second sprint")
			.startDate(LocalDate.now())
			.endDate(LocalDate.now().plusDays(1))
			.build();

		// when
		CreateSprintResponse secondSprintResponse = sprintCommandService.createSprint(workspace.getCode(), request);

		// then
		assertThat(firstSprintResponse.sprintKey()).isEqualTo("SPRINT-1");
		assertThat(secondSprintResponse.sprintKey()).isEqualTo("SPRINT-2");
	}

	@Test
	@DisplayName("여러개의 이슈를 한번에 스프린트에 등록할 수 있다")
	void canAddMultipleIssuesAsBulkToSprint() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		AddSprintIssuesRequest request = new AddSprintIssuesRequest(
			List.of(issue1.getIssueKey(), issue2.getIssueKey())
		);

		// when
		AddSprintIssuesResponse response = sprintCommandService.addIssues(
			workspace.getCode(),
			sprint.getSprintKey(),
			request
		);

		// then
		assertThat(response.sprintKey()).isEqualTo(sprint.getSprintKey());
		assertThat(response.addedIssueKeys()).isEqualTo(List.of(issue1.getIssueKey(), issue2.getIssueKey()));
	}

	@Test
	@DisplayName("이슈를 스프린트에 추가하는 경우, 이슈 키 중 하나라도 유효하지 않다면 실패한다")
	void addingIssuesToSprintFails_IfInvalidIssueKeyExists() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("test sprint")
			.goal("test sprint")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		AddSprintIssuesRequest request = new AddSprintIssuesRequest(
			List.of(issue1.getIssueKey(), issue2.getIssueKey(), "INVALID-KEY")
		);

		// when & then
		assertThatThrownBy(() -> sprintCommandService.addIssues(
				workspace.getCode(),
				sprint.getSprintKey(),
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
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);

		// when
		sprintCommandService.removeIssue(
			workspace.getCode(),
			sprint.getSprintKey(),
			new RemoveSprintIssueRequest(issue1.getIssueKey())
		);

		// then
		Sprint foundSprint = sprintRepository.findBySprintKeyAndWorkspaceCode(
				sprint.getSprintKey(),
				workspace.getCode())
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
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		UpdateSprintContentRequest request = UpdateSprintContentRequest.builder()
			.title("updated title")
			.goal("updated goal")
			.build();

		// when
		UpdateSprintContentResponse response = sprintCommandService.updateSprintContent(
			workspace.getCode(),
			sprint.getSprintKey(),
			request
		);

		// then
		assertThat(response.sprintKey()).isEqualTo(sprint.getSprintKey());
		assertThat(response.title()).isEqualTo("updated title");
		assertThat(response.goal()).isEqualTo("updated goal");
	}

	@Test
	@DisplayName("스프린트의 내용 업데이트 시, 요청에서 값을 명시한 필드만 업데이트된다(만약 goal의 값이 없으면 업데이트 대상이 아니다)")
	void onlyRequestFieldsNotNullAreUpdated() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		// leave out value of goal
		UpdateSprintContentRequest request = UpdateSprintContentRequest.builder()
			.title("updated title")
			.build();

		// when
		UpdateSprintContentResponse response = sprintCommandService.updateSprintContent(
			workspace.getCode(),
			sprint.getSprintKey(),
			request
		);

		// then
		assertThat(response.sprintKey()).isEqualTo(sprint.getSprintKey());
		assertThat(response.title()).isEqualTo("updated title");
		assertThat(response.goal()).isEqualTo("original goal");
	}

	@Test
	@DisplayName("스프린트의 시작일자, 종료일자(startDate, endDate)를 업데이트할 수 있다")
	void canUpdateSprintDates() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		UpdateSprintDateRequest request = new UpdateSprintDateRequest(
			LocalDate.now().plusDays(7),
			LocalDate.now().plusDays(14)
		);

		// when
		UpdateSprintDateResponse response = sprintCommandService.updateSprintDate(
			workspace.getCode(),
			sprint.getSprintKey(),
			request
		);

		// then
		assertThat(response.sprintKey()).isEqualTo(sprint.getSprintKey());
		assertThat(response.startDate()).isEqualTo(LocalDate.now().plusDays(7));
		assertThat(response.endDate()).isEqualTo(LocalDate.now().plusDays(14));
	}

	@Test
	@DisplayName("스프린트의 종료일자를 시작일자 이전으로 업데이트할 수 없다")
	void cannotUpdateSprintEndDateToBeBeforeStartDate() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		// endDate is 7 days before startDate
		UpdateSprintDateRequest request = new UpdateSprintDateRequest(
			LocalDate.now().plusDays(14),
			LocalDate.now().plusDays(7)
		);

		// when & then
		assertThatThrownBy(() -> sprintCommandService.updateSprintDate(
				workspace.getCode(),
				sprint.getSprintKey(),
				request
			)
		).isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("PLANNING에서 ACTIVE로 스프린트의 상태를 변경할 수 있다(스프린트를 시작할 수 있다)")
	void canChangeStatusOfSprintFromPlanningToActive() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		// when
		UpdateSprintStatusResponse response = sprintCommandService.updateSprintStatus(
			workspace.getCode(),
			sprint.getSprintKey(),
			new UpdateSprintStatusRequest(SprintStatus.ACTIVE)
		);

		// then
		assertThat(response.status()).isEqualTo(SprintStatus.ACTIVE);
	}

	@Test
	@DisplayName("PLANNING에서 CANCELLED로 스프린트의 상태를 변경할 수 있다(스프린트를 취소할 수 있다)")
	void canChangeStatusOfSprintFromPlanningToCancelled() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		// when
		UpdateSprintStatusResponse response = sprintCommandService.updateSprintStatus(
			workspace.getCode(),
			sprint.getSprintKey(),
			new UpdateSprintStatusRequest(SprintStatus.CANCELLED)
		);

		// then
		assertThat(response.status()).isEqualTo(SprintStatus.CANCELLED);
	}

	@Test
	@DisplayName("현재 날짜가 종료일자를 지났으면, PLANNING에서 ACTIVE로 스프린트의 상태를 변경할 수 없다(스프린트를 시작할 수 없다)")
	void cannotChangeStatusOfSprintFromPlanningToActive_IfEndDateHasPassed() {
		// given - sprint ended yesterday
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().minusDays(1))
			.workspace(workspace)
			.build());

		// when & then
		assertThatThrownBy(() -> sprintCommandService.updateSprintStatus(
				workspace.getCode(),
				sprint.getSprintKey(),
				new UpdateSprintStatusRequest(SprintStatus.ACTIVE)
			)
		).isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("이미 설정되어 있던 상태로 스프린트의 상태를 변경할 수 없다")
	void cannotChangeStatusToSameStatus() {
		// given
		Sprint sprint = sprintRepository.save(Sprint.builder()
			.title("original title")
			.goal("original goal")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		// when & then
		assertThatThrownBy(() -> sprintCommandService.updateSprintStatus(
				workspace.getCode(),
				sprint.getSprintKey(),
				new UpdateSprintStatusRequest(SprintStatus.PLANNING)
			)
		).isInstanceOf(InvalidOperationException.class);
	}

	@ParameterizedTest
	@EnumSource(value = SprintStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("ACTIVE에서 COMPLETED 또는 CANCELLED로 스프린트의 상태를 변경할 수 있다(진행되고 있는 스프린트를 완료하거나 취소할 수 있다)")
	void canCompleteOrCancel_ActiveSprint(SprintStatus status) {
		// given
		Sprint sprint = Sprint.builder()
			.title("original title")
			.goal("original goal")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build();
		sprint.updateStatus(SprintStatus.ACTIVE);
		sprintRepository.save(sprint);

		// when
		UpdateSprintStatusResponse response = sprintCommandService.updateSprintStatus(
			workspace.getCode(),
			sprint.getSprintKey(),
			new UpdateSprintStatusRequest(status)
		);

		// then
		assertThat(response.status()).isEqualTo(status);
	}

	@ParameterizedTest
	@EnumSource(value = SprintStatus.class, names = {"COMPLETED", "CANCELLED"})
	@DisplayName("COMPLETED 또는 CANCELLED된 스프린트의 상태를 변경할 수 없다(완료되거나 취소된 스프린트의 상태를 변경할 수 없다)")
	void cannotChangeCompletedOrCancelledSprintStatus(SprintStatus status) {
		// given
		Sprint sprint = Sprint.builder()
			.title("original title")
			.goal("original goal")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build();
		sprint.updateStatus(SprintStatus.ACTIVE);
		sprint.updateStatus(status);
		sprintRepository.save(sprint);

		// when & then
		assertThatThrownBy(() -> sprintCommandService.updateSprintStatus(
			workspace.getCode(),
			sprint.getSprintKey(),
			new UpdateSprintStatusRequest(SprintStatus.PLANNING)
		)).isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("워크스페이스 내에서는 동시에 하나의 스프린트만 ACTIVE로 존재할 수 있다(동시에 하나의 스프린트만 진행 가능)")
	void onlyASingleSprintCanBeActiveSimultaneously() {
		// given
		Sprint sprint1 = Sprint.builder()
			.title("sprint 1")
			.goal("sprint 1")
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build();

		sprint1.updateStatus(SprintStatus.ACTIVE);
		sprintRepository.save(sprint1);

		Sprint sprint2 = sprintRepository.save(Sprint.builder()
			.title("sprint 2")
			.goal("sprint 2")
			.startDate(LocalDate.now().plusDays(2))
			.endDate(LocalDate.now().plusDays(3))
			.workspace(workspace)
			.build());

		// when & then
		assertThatThrownBy(() -> sprintCommandService.updateSprintStatus(
				workspace.getCode(),
				sprint2.getSprintKey(),
				new UpdateSprintStatusRequest(SprintStatus.ACTIVE)
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
			.startDate(LocalDate.of(2025, 1, 1))
			.endDate(LocalDate.now().plusDays(1))
			.workspace(workspace)
			.build());

		sprint.addIssue(issue1);
		sprint.addIssue(issue2);

		sprint.updateStatus(SprintStatus.ACTIVE);
		sprint.updateStatus(status);

		// when & then
		assertThatThrownBy(() -> sprintCommandService.removeIssue(
				workspace.getCode(),
				sprint.getSprintKey(),
				new RemoveSprintIssueRequest(issue1.getIssueKey())
			)
		).isInstanceOf(InvalidOperationException.class);
	}
}
