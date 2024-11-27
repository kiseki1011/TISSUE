package com.uranus.taskmanager.api.workspace.service;

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
import com.uranus.taskmanager.api.member.presentation.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.security.authentication.presentation.dto.LoginMember;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.WorkspaceJoinRequest;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class WorkspaceQueryServiceTest extends ServiceIntegrationTestHelper {

	@BeforeEach
	void setup() {
		// member1 회원가입
		memberService.signup(SignupMemberRequest.builder()
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
		workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			null);
		workspaceRepositoryFixture.createWorkspace("workspace2", "description2", "TEST2222",
			null);

		// member1은 workspace1,2에 참여
		workspaceMemberService.joinWorkspace("TEST1111", new WorkspaceJoinRequest(), loginMember1.getId());
		workspaceMemberService.joinWorkspace("TEST2222", new WorkspaceJoinRequest(), loginMember1.getId());
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
		MyWorkspacesResponse response = memberQueryService.getMyWorkspaces(1L, pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(2);
	}

	@Transactional
	@Test
	@DisplayName("멤버는 자기가 참여한 모든 워크스페이스를 조회할 수 있다(자기가 생성하지 않은 워크스페이스)")
	void test2() {
		// given
		memberService.signup(SignupMemberRequest.builder()
			.loginId("member2")
			.email("member2@test.com")
			.password("member2password!")
			.build());

		LoginMember loginMember2 = LoginMember.builder()
			.id(2L)
			.loginId("member2")
			.email("member2@test.com")
			.build();

		workspaceMemberService.joinWorkspace("TEST1111", new WorkspaceJoinRequest(), loginMember2.getId());
		Pageable pageable = PageRequest.of(0, 20);

		// when
		MyWorkspacesResponse response = memberQueryService.getMyWorkspaces(2L, pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(1);
	}

	@Transactional
	@Test
	@DisplayName("해당 워크스페이스에 참여하고 있으면, 워크스페이스의 코드로 상세 정보를 조회할 수 있다")
	void test3() {
		// given
		memberService.signup(SignupMemberRequest.builder()
			.loginId("member2")
			.email("member2@test.com")
			.password("member2password!")
			.build());

		LoginMember loginMember2 = LoginMember.builder()
			.id(2L)
			.loginId("member2")
			.email("member2@test.com")
			.build();

		workspaceMemberService.joinWorkspace("TEST1111", new WorkspaceJoinRequest(), loginMember2.getId());

		// when
		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail("TEST1111", 2L);

		// then
		assertThat(response.getCode()).isEqualTo("TEST1111");
		assertThat(response.getName()).isEqualTo("workspace1");
	}

	@Transactional
	@Test
	@DisplayName("해당 워크스페이스에 참여하지 않으면, 유효한 코드로 상세 정보를 조회해도 예외가 발생한다")
	void test4() {
		// given
		memberService.signup(SignupMemberRequest.builder()
			.loginId("member2")
			.email("member2@test.com")
			.password("member2password!")
			.build());

		LoginMember loginMember2 = LoginMember.builder()
			.id(2L)
			.loginId("member2")
			.email("member2@test.com")
			.build();

		// when & then
		assertThatThrownBy(() -> workspaceQueryService.getWorkspaceDetail("TEST1111", loginMember2.getId()))
			.isInstanceOf(MemberNotInWorkspaceException.class);
	}

	@Transactional
	@Test
	@DisplayName("유효하지 않은 코드로 워크스페이스를 조회하면 예외가 발생한다")
	void test5() {
		// given
		memberService.signup(SignupMemberRequest.builder()
			.loginId("member2")
			.email("member2@test.com")
			.password("member2password!")
			.build());

		LoginMember loginMember2 = LoginMember.builder()
			.id(2L)
			.loginId("member2")
			.email("member2@test.com")
			.build();

		// when & then
		assertThatThrownBy(() -> workspaceQueryService.getWorkspaceDetail("BADCODE1", loginMember2.getId()))
			.isInstanceOf(WorkspaceNotFoundException.class);
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 전체 조회에서 이름에 대한 역정렬을 적용하면, 역정렬된 결과로 조회할 수 있다")
	void test6() {
		// given
		memberService.signup(SignupMemberRequest.builder()
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
			Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace" + i, "description" + i,
				"TEST" + i, null);
			workspaceRepositoryFixture.addMemberToWorkspace(member2, workspace, WorkspaceRole.MANAGER);
		}

		// 워크스페이스 name 기준 역정렬을 하기 위한 PageRequest
		Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "workspace.name"));

		// when
		MyWorkspacesResponse response = memberQueryService.getMyWorkspaces(loginMember2.getId(), pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(5);

		// 역정렬 되었는지 검증
		List<WorkspaceDetail> workspaces = response.getWorkspaces();
		List<String> expectedOrder = Arrays.asList("workspace7", "workspace6",
			"workspace5", "workspace4", "workspace3");

		for (int i = 0; i < workspaces.size(); i++) {
			assertThat(workspaces.get(i).getName()).isEqualTo(expectedOrder.get(i));
		}
	}
}
