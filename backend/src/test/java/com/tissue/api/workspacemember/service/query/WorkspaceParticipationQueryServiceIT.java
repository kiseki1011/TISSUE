package com.tissue.api.workspacemember.service.query;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.presentation.dto.WorkspaceDetail;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.response.MyWorkspacesResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

class WorkspaceParticipationQueryServiceIT extends ServiceIntegrationTestHelper {

	Member member1;
	Workspace workspace1;
	Workspace workspace2;

	@BeforeEach
	void setup() {
		// create member1
		member1 = testDataFixture.createMember("member1");

		// create workspace1, workspace2
		workspace1 = testDataFixture.createWorkspace("workspace1", null, null);
		workspace2 = testDataFixture.createWorkspace("workspace2", null, null);

		// member1 joined workspace1, workspace2
		testDataFixture.createWorkspaceMember(member1, workspace1, WorkspaceRole.MEMBER);
		testDataFixture.createWorkspaceMember(member1, workspace2, WorkspaceRole.MEMBER);
	}

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@Transactional
	@DisplayName("멤버는 자기가 참여한 모든 워크스페이스를 조회할 수 있다")
	void memberCanQueryAllJoinedWorkspaces() {
		// given
		Pageable pageable = PageRequest.of(0, 20);

		// when
		MyWorkspacesResponse response = workspaceParticipationQueryService.getMyWorkspaces(member1.getId(), pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(2);
	}

	@Test
	@Transactional
	@DisplayName("워크스페이스 전체 조회에서 이름에 대한 역정렬된 결과로 조회할 수 있다")
	void memberCanQueryAllJoinedWorkspacesByNameInDescendingOrder() {
		// given
		Member member2 = testDataFixture.createMember("member2");

		// create 5 workspaces (workspace3 ~ 7)
		for (int i = 3; i <= 7; i++) {
			Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
				"workspace" + i,
				"description" + i,
				"TEST" + i,
				null
			);
			workspaceRepositoryFixture.addAndSaveMemberToWorkspace(member2, workspace, WorkspaceRole.MANAGER);
		}

		// PageRequest for descending order by name
		Pageable pageable = PageRequest.of(
			0,
			20,
			Sort.by(Sort.Direction.DESC, "workspace.name")
		);

		// when
		MyWorkspacesResponse response = workspaceParticipationQueryService.getMyWorkspaces(member2.getId(), pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(5);

		// verify if sorted by descending order
		List<WorkspaceDetail> workspaces = response.getWorkspaces();
		List<String> expectedOrder = Arrays.asList(
			"workspace7",
			"workspace6",
			"workspace5",
			"workspace4",
			"workspace3"
		);

		for (int i = 0; i < workspaces.size(); i++) {
			assertThat(workspaces.get(i).getName()).isEqualTo(expectedOrder.get(i));
		}
	}
}