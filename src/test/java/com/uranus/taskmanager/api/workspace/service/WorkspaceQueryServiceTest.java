package com.uranus.taskmanager.api.workspace.service;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceParticipateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
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
		Assertions.assertThat(response.getWorkspaceCount()).isEqualTo(2);
	}

	@Transactional
	@Test
	@DisplayName("멤버는 자기가 참여한 모든 워크스페이스를 조회할 수 있다(자기가 생성하지 않은 워크스페이스)")
	void test2() {
		// given
		Member member2 = memberRespositoryFixture.createMember("member2", "member2@test.com", "member2password!");
		LoginMemberDto loginMember2 = LoginMemberDto.builder()
			.loginId("member2")
			.email("member2@test.com")
			.build();

		workspaceService.participateWorkspace("TEST1111", new WorkspaceParticipateRequest(), loginMember2);

		// when
		MyWorkspacesResponse response = workspaceQueryService.getMyWorkspaces(loginMember2);

		// then
		Assertions.assertThat(response.getWorkspaceCount()).isEqualTo(1);
	}
}
