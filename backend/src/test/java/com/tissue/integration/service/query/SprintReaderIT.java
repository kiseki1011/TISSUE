package com.tissue.integration.service.query;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.types.Story;
import com.tissue.api.member.domain.Member;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

public class SprintReaderIT extends ServiceIntegrationTestHelper {

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
	@Transactional
	@DisplayName("스프린트 키(sprintKey)를 통해서 스프린트를 조회할 수 있다")
	void canQuerySprintBySprintKey() {
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
		Sprint foundSprint = sprintReader.findSprint(sprint.getSprintKey(), workspace.getCode());

		// then
		assertThat(foundSprint.getSprintKey()).isEqualTo(sprint.getSprintKey());
	}

	/**
	 * check SQL query in terminal
	 */
	@Test
	@Transactional
	@DisplayName("스프린트 조회 시 연관된 이슈도 함께 1차 캐시로 가져올 수 있다")
	void canBringAssociatedIssuesToPersistenceContext_WhenQueryingSprint() {
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
		Sprint foundSprint = sprintReader.findSprintWithIssues(sprint.getSprintKey(), workspace.getCode());

		// then
		assertThat(foundSprint.getSprintKey()).isEqualTo(sprint.getSprintKey());
		assertThat(foundSprint.getSprintIssues()).isNotEmpty();
	}
}

