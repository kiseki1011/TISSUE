package com.uranus.taskmanager.api.workspace.service;

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceParticipateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.exception.MemberNotInWorkspaceException;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.fixture.repository.MemberRespositoryFixture;
import com.uranus.taskmanager.fixture.repository.WorkspaceRepositoryFixture;

@SpringBootTest
public class WorkspaceQueryServiceTest {

	@Autowired
	private WorkspaceAccessService workspaceAccessService;
	@Autowired
	private WorkspaceQueryService workspaceQueryService;
	@Autowired
	private WorkspaceRepository workspaceRepository;
	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;
	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private WorkspaceRepositoryFixture workspaceRepositoryFixture;
	@Autowired
	private MemberRespositoryFixture memberRespositoryFixture;

	@BeforeEach
	void setup() {
		// member1이 workspace1,2를 생성
		Member member1 = memberRespositoryFixture.createMember("member1", "member1@test.com", "member1password!");
		Workspace workspace1 = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			null);
		Workspace workspace2 = workspaceRepositoryFixture.createWorkspace("workspace2", "description2", "TEST2222",
			null);
		workspaceRepositoryFixture.addMemberToWorkspace(member1, workspace1, WorkspaceRole.ADMIN);
		workspaceRepositoryFixture.addMemberToWorkspace(member1, workspace2, WorkspaceRole.ADMIN);
	}

	@AfterEach
	void tearDown() {
		workspaceMemberRepository.deleteAll();
		workspaceRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Transactional
	@Test
	@DisplayName("멤버는 자기가 참여한 모든 워크스페이스를 조회할 수 있다(자기가 생성한 워크스페이스)")
	void test1() {
		// given
		LoginMemberDto loginMember1 = LoginMemberDto.builder()
			.loginId("member1")
			.email("member1@test.com")
			.build();
		Pageable pageable = PageRequest.of(0, 20);

		// when
		MyWorkspacesResponse response = workspaceQueryService.getMyWorkspaces(loginMember1, pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(2);
	}

	@Transactional
	@Test
	@DisplayName("멤버는 자기가 참여한 모든 워크스페이스를 조회할 수 있다(자기가 생성하지 않은 워크스페이스)")
	void test2() {
		// given
		memberRespositoryFixture.createMember("member2", "member2@test.com", "member2password!");
		LoginMemberDto loginMember2 = LoginMemberDto.builder()
			.loginId("member2")
			.email("member2@test.com")
			.build();

		workspaceAccessService.joinWorkspace("TEST1111", new WorkspaceParticipateRequest(), loginMember2);
		Pageable pageable = PageRequest.of(0, 20);

		// when
		MyWorkspacesResponse response = workspaceQueryService.getMyWorkspaces(loginMember2, pageable);

		// then
		assertThat(response.getTotalElements()).isEqualTo(1);
	}

	@Transactional
	@Test
	@DisplayName("해당 워크스페이스에 참여하고 있으면, 워크스페이스의 코드로 상세 정보를 조회할 수 있다")
	void test3() {
		// given
		memberRespositoryFixture.createMember("member2", "member2@test.com", "member2password!");
		LoginMemberDto loginMember2 = LoginMemberDto.builder()
			.loginId("member2")
			.email("member2@test.com")
			.build();

		workspaceAccessService.joinWorkspace("TEST1111", new WorkspaceParticipateRequest(), loginMember2);

		// when
		WorkspaceDetail response = workspaceQueryService.getWorkspaceDetail("TEST1111", loginMember2);

		// then
		assertThat(response.getCode()).isEqualTo("TEST1111");
		assertThat(response.getName()).isEqualTo("workspace1");
	}

	@Transactional
	@Test
	@DisplayName("해당 워크스페이스에 참여하지 않으면, 유효한 코드로 상세 정보를 조회해도 예외가 발생한다")
	void test4() {
		// given
		memberRespositoryFixture.createMember("member2", "member2@test.com", "member2password!");
		LoginMemberDto loginMember2 = LoginMemberDto.builder()
			.loginId("member2")
			.email("member2@test.com")
			.build();

		// when & then
		assertThatThrownBy(() -> workspaceQueryService.getWorkspaceDetail("TEST1111", loginMember2))
			.isInstanceOf(MemberNotInWorkspaceException.class);
	}

	@Transactional
	@Test
	@DisplayName("유효하지 않은 코드로 워크스페이스를 조회하면 예외가 발생한다")
	void test5() {
		// given
		memberRespositoryFixture.createMember("member2", "member2@test.com", "member2password!");
		LoginMemberDto loginMember2 = LoginMemberDto.builder()
			.loginId("member2")
			.email("member2@test.com")
			.build();

		// when & then
		assertThatThrownBy(() -> workspaceQueryService.getWorkspaceDetail("BADCODE1", loginMember2))
			.isInstanceOf(WorkspaceNotFoundException.class);
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 전체 조회에서 이름에 대한 역정렬을 적용하면, 역정렬된 결과로 조회할 수 있다")
	void test6() {
		// given
		Member member2 = memberRespositoryFixture.createMember("member2", "member2@test.com", "member1password!");
		LoginMemberDto loginMember2 = LoginMemberDto.builder()
			.loginId("member2")
			.email("member2@test.com")
			.build();

		/*
		 * workspace3 ~ 7 이름으로 워크스페이스 5개 생성
		 */
		for (int i = 3; i <= 7; i++) {
			Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace" + i, "description" + i,
				"TEST" + i, null);
			workspaceRepositoryFixture.addMemberToWorkspace(member2, workspace, WorkspaceRole.ADMIN);
		}

		// 워크스페이스 name 기준 역정렬을 하기 위한 PageRequest
		Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "workspace.name"));

		// when
		MyWorkspacesResponse response = workspaceQueryService.getMyWorkspaces(loginMember2, pageable);

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
