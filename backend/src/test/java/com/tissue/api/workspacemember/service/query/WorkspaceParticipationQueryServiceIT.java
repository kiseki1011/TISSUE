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
import com.tissue.api.member.presentation.dto.request.SignupMemberRequest;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.presentation.dto.WorkspaceDetail;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.presentation.dto.request.JoinWorkspaceRequest;
import com.tissue.api.workspacemember.presentation.dto.response.MyWorkspacesResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

class WorkspaceParticipationQueryServiceIT extends ServiceIntegrationTestHelper {

	@BeforeEach
	void setup() {
		// member1 회원가입
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"member1",
			"member1@test.com",
			"member1password!"
		);
		memberCommandService.signup(signupMemberRequest);

		// workspace1, workspace2 생성
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TEST1111",
			null
		);
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace2",
			"description2",
			"TEST2222",
			null
		);

		// member1은 workspace1,2에 참여
		workspaceParticipationCommandService.joinWorkspace("TEST1111", new JoinWorkspaceRequest(), 1L);
		workspaceParticipationCommandService.joinWorkspace("TEST2222", new JoinWorkspaceRequest(), 1L);
	}

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Transactional
	@Test
	@DisplayName("멤버는 자기가 참여한 모든 워크스페이스를 조회할 수 있다")
	void test1() {
		// given
		Pageable pageable = PageRequest.of(0, 20);

		// when
		MyWorkspacesResponse response = workspaceParticipationQueryService.getMyWorkspaces(1L, pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(2);
	}

	@Transactional
	@Test
	@DisplayName("멤버는 자기가 참여한 모든 워크스페이스를 조회할 수 있다(자기가 생성하지 않은 워크스페이스)")
	void test2() {
		// given
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"member2",
			"member2@test.com",
			"member2password!!"
		);
		memberCommandService.signup(signupMemberRequest);

		workspaceParticipationCommandService.joinWorkspace(
			"TEST1111",
			new JoinWorkspaceRequest(),
			2L
		);

		Pageable pageable = PageRequest.of(0, 20);

		// when
		MyWorkspacesResponse response = workspaceParticipationQueryService.getMyWorkspaces(2L, pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(1);
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 전체 조회에서 이름에 대한 역정렬을 적용하면, 역정렬된 결과로 조회할 수 있다")
	void test6() {
		// given
		SignupMemberRequest signupMemberRequest = signupRequestDtoFixture.createSignupRequest(
			"member2",
			"member2@test.com",
			"member2password!!"
		);
		memberCommandService.signup(signupMemberRequest);

		Member member2 = memberRepository.findByLoginId("member2").get();

		// workspace3 ~ 7 이라는 이름으로 워크스페이스 5개 생성
		for (int i = 3; i <= 7; i++) {
			Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
				"workspace" + i,
				"description" + i,
				"TEST" + i,
				null
			);
			workspaceRepositoryFixture.addAndSaveMemberToWorkspace(member2, workspace, WorkspaceRole.MANAGER);
		}

		// 워크스페이스 name 기준 역정렬을 하기 위한 PageRequest
		Pageable pageable = PageRequest.of(
			0,
			20,
			Sort.by(Sort.Direction.DESC, "workspace.name")
		);

		// when
		MyWorkspacesResponse response = workspaceParticipationQueryService.getMyWorkspaces(member2.getId(), pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(5);

		// 역정렬 되었는지 검증
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