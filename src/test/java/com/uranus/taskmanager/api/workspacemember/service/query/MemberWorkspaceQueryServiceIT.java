package com.uranus.taskmanager.api.workspacemember.service.query;

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

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.presentation.dto.request.SignupMemberRequest;
import com.uranus.taskmanager.api.security.authentication.resolver.LoginMember;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.JoinWorkspaceRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.GetMyWorkspacesResponse;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class MemberWorkspaceQueryServiceIT extends ServiceIntegrationTestHelper {

	@BeforeEach
	void setup() {
		// member1 회원가입
		memberCommandService.signup(SignupMemberRequest.builder()
			.loginId("member1")
			.email("member1@test.com")
			.password("member1password!")
			.build());

		LoginMember loginMember1 = LoginMember.builder()
			.id(1L)
			.loginId("member1")
			.email("member1@test.com")
			.build();

		// workspace1, workspace2 생성
		workspaceRepositoryFixture.createWorkspace(
			"workspace1",
			"description1",
			"TEST1111",
			null
		);
		workspaceRepositoryFixture.createWorkspace(
			"workspace2",
			"description2",
			"TEST2222",
			null
		);

		// member1은 workspace1,2에 참여
		memberWorkspaceCommandService.joinWorkspace("TEST1111", new JoinWorkspaceRequest(), loginMember1.getId());
		memberWorkspaceCommandService.joinWorkspace("TEST2222", new JoinWorkspaceRequest(), loginMember1.getId());
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
		GetMyWorkspacesResponse response = memberWorkspaceQueryService.getMyWorkspaces(1L, pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(2);
	}

	@Transactional
	@Test
	@DisplayName("멤버는 자기가 참여한 모든 워크스페이스를 조회할 수 있다(자기가 생성하지 않은 워크스페이스)")
	void test2() {
		// given
		memberCommandService.signup(SignupMemberRequest.builder()
			.loginId("member2")
			.email("member2@test.com")
			.password("member2password!")
			.build());

		LoginMember loginMember2 = LoginMember.builder()
			.id(2L)
			.loginId("member2")
			.email("member2@test.com")
			.build();

		memberWorkspaceCommandService.joinWorkspace(
			"TEST1111",
			new JoinWorkspaceRequest(),
			loginMember2.getId()
		);

		Pageable pageable = PageRequest.of(0, 20);

		// when
		GetMyWorkspacesResponse response = memberWorkspaceQueryService.getMyWorkspaces(2L, pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(1);
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 전체 조회에서 이름에 대한 역정렬을 적용하면, 역정렬된 결과로 조회할 수 있다")
	void test6() {
		// given
		memberCommandService.signup(SignupMemberRequest.builder()
			.loginId("member2")
			.email("member2@test.com")
			.password("member2password!")
			.build());

		LoginMember loginMember2 = LoginMember.builder()
			.id(2L)
			.loginId("member2")
			.email("member2@test.com")
			.build();

		Member member2 = memberRepository.findByLoginId("member2").get();

		// workspace3 ~ 7 이라는 이름으로 워크스페이스 5개 생성
		for (int i = 3; i <= 7; i++) {
			Workspace workspace = workspaceRepositoryFixture.createWorkspace(
				"workspace" + i,
				"description" + i,
				"TEST" + i,
				null
			);
			workspaceRepositoryFixture.addMemberToWorkspace(member2, workspace, WorkspaceRole.MANAGER);
		}

		// 워크스페이스 name 기준 역정렬을 하기 위한 PageRequest
		Pageable pageable = PageRequest.of(
			0,
			20,
			Sort.by(Sort.Direction.DESC, "workspace.name")
		);

		// when
		GetMyWorkspacesResponse response = memberWorkspaceQueryService.getMyWorkspaces(loginMember2.getId(), pageable);

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