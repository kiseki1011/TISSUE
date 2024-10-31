package com.uranus.taskmanager.api.workspace.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.member.domain.Member;
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
	private WorkspaceService workspaceService;
	@Autowired
	private WorkspaceQueryService workspaceQueryService;
	@Autowired
	private WorkspaceRepository workspaceRepository;
	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;

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

	@Transactional
	@Test
	@DisplayName("멤버는 자기가 참여한 모든 워크스페이스를 조회할 수 있다(자기가 생성한 워크스페이스)")
	void test1() {
		// given
		LoginMemberDto loginMember1 = LoginMemberDto.builder()
			.loginId("member1")
			.email("member1@test.com")
			.build();

		// when
		MyWorkspacesResponse response = workspaceQueryService.getMyWorkspaces(loginMember1);

		// then
		assertThat(response.getWorkspaceCount()).isEqualTo(2);
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

		workspaceService.participateWorkspace("TEST1111", new WorkspaceParticipateRequest(), loginMember2);

		// when
		MyWorkspacesResponse response = workspaceQueryService.getMyWorkspaces(loginMember2);

		// then
		assertThat(response.getWorkspaceCount()).isEqualTo(1);
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

		workspaceService.participateWorkspace("TEST1111", new WorkspaceParticipateRequest(), loginMember2);

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
}
