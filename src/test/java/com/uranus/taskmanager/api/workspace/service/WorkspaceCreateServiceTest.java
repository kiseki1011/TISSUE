package com.uranus.taskmanager.api.workspace.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.uranus.taskmanager.api.authentication.dto.LoginMember;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceCreateResponse;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceCodeCollisionHandleException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.util.WorkspaceCodeGenerator;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.fixture.dto.LoginMemberDtoFixture;
import com.uranus.taskmanager.fixture.dto.WorkspaceCreateDtoFixture;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceMemberEntityFixture;

@ExtendWith(MockitoExtension.class)
class WorkspaceCreateServiceTest {

	@InjectMocks
	private CheckCodeDuplicationService workspaceCreateService;

	@Mock
	private WorkspaceCodeGenerator workspaceCodeGenerator;
	@Mock
	private WorkspaceRepository workspaceRepository;
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private WorkspaceMemberRepository workspaceMemberRepository;
	@Mock
	private PasswordEncoder passwordEncoder;

	WorkspaceEntityFixture workspaceEntityFixture;
	MemberEntityFixture memberEntityFixture;
	WorkspaceMemberEntityFixture workspaceMemberEntityFixture;
	LoginMemberDtoFixture loginMemberDtoFixture;
	WorkspaceCreateDtoFixture workspaceCreateDtoFixture;

	@BeforeEach
	public void setup() {
		workspaceMemberEntityFixture = new WorkspaceMemberEntityFixture();
		memberEntityFixture = new MemberEntityFixture();
		loginMemberDtoFixture = new LoginMemberDtoFixture();
		workspaceEntityFixture = new WorkspaceEntityFixture();
		workspaceCreateDtoFixture = new WorkspaceCreateDtoFixture();
	}

	@Test
	@DisplayName("워크스페이스 생성에는 생성 요청과 로그인 멤버를 필요로 한다")
	void test1() {
		// given
		WorkspaceCreateRequest request = workspaceCreateDtoFixture.createWorkspaceCreateRequest(null);
		String code = "TESTCODE";

		Workspace workspace = workspaceEntityFixture.createWorkspace("testcode");
		Member member = memberEntityFixture.createMember("user123", "test@test.com");
		LoginMember loginMember = loginMemberDtoFixture.createLoginMemberDto(1L, "user123", "test@test.com");

		when(memberRepository.findById(loginMember.getId())).thenReturn(Optional.of(member));
		when(workspaceCodeGenerator.generateWorkspaceCode()).thenReturn(code);
		when(workspaceRepository.save(any(Workspace.class))).thenReturn(workspace);

		// when
		WorkspaceCreateResponse response = workspaceCreateService.createWorkspace(request, loginMember.getId());

		// then
		assertThat(response).isNotNull();
		verify(memberRepository, times(1)).findById(1L);
	}

	@DisplayName("워크스페이스 생성을 성공하면 WorkspaceResponse를 반환한다")
	void test2() {
		// given
		WorkspaceCreateRequest request = workspaceCreateDtoFixture.createWorkspaceCreateRequest(null);
		String code = "TESTCODE";

		Workspace workspace = workspaceEntityFixture.createWorkspace("testcode");
		Member member = memberEntityFixture.createMember("user123", "test@test.com");
		LoginMember loginMember = loginMemberDtoFixture.createLoginMemberDto(1L, "user123", "test@test.com");

		when(memberRepository.findById(loginMember.getId())).thenReturn(Optional.of(member));
		when(workspaceCodeGenerator.generateWorkspaceCode()).thenReturn(code);
		when(workspaceRepository.save(any(Workspace.class))).thenReturn(workspace);

		// when
		WorkspaceCreateResponse response = workspaceCreateService.createWorkspace(request, loginMember.getId());

		// then
		assertThat(response).isNotNull();
		assertThat(response).isInstanceOf(WorkspaceCreateResponse.class);
		verify(workspaceRepository, times(1)).save(any(Workspace.class));
	}

	@Test
	@DisplayName("워크스페이스 코드가 중복될 때 최대 재시도 횟수(5회)를 소진하면 예외가 발생한다")
	void test3() {
		// given
		WorkspaceCreateRequest request = workspaceCreateDtoFixture.createWorkspaceCreateRequest(null);

		Member member = memberEntityFixture.createMember("user123", "test@test.com");
		LoginMember loginMember = loginMemberDtoFixture.createLoginMemberDto(1L, "user123", "test@test.com");
		when(memberRepository.findById(loginMember.getId())).thenReturn(Optional.of(member));

		when(workspaceCodeGenerator.generateWorkspaceCode())
			.thenReturn("WORK123", "WORK124", "WORK125", "WORK126", "WORK127");
		when(workspaceRepository.existsByCode(anyString())).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> workspaceCreateService.createWorkspace(request, loginMember.getId()))
			.isInstanceOf(WorkspaceCodeCollisionHandleException.class)
			.hasMessageContaining("Failed to solve workspace code collision");
	}

	@Test
	@DisplayName("워크스페이스 생성 시 WorkspaceMember도 생성되고 저장된다")
	void test4() {
		// given
		WorkspaceCreateRequest request = workspaceCreateDtoFixture.createWorkspaceCreateRequest(null);
		String code = "TESTCODE";

		Workspace workspace = workspaceEntityFixture.createWorkspace("testcode");
		Member member = memberEntityFixture.createMember("user123", "test@test.com");
		LoginMember loginMember = loginMemberDtoFixture.createLoginMemberDto(1L, "user123", "test@test.com");
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createAdminWorkspaceMember(member, workspace);
		when(memberRepository.findById(loginMember.getId())).thenReturn(Optional.of(member));
		when(workspaceCodeGenerator.generateWorkspaceCode()).thenReturn(code);
		when(workspaceRepository.save(any(Workspace.class))).thenReturn(workspace);
		when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(workspaceMember);

		// when
		workspaceCreateService.createWorkspace(request, loginMember.getId());

		// then
		verify(workspaceMemberRepository, times(1)).save(any(WorkspaceMember.class));
	}

	@Test
	@DisplayName("워크스페이스 생성 시 WorkspaceMember의 별칭은 생성자의 이메일로 설정된다")
	void test5() {
		// given
		WorkspaceCreateRequest request = workspaceCreateDtoFixture.createWorkspaceCreateRequest(null);
		String code = "TESTCODE";

		Workspace workspace = workspaceEntityFixture.createWorkspace("testcode");
		Member member = memberEntityFixture.createMember("user123", "test@test.com");
		LoginMember loginMember = loginMemberDtoFixture.createLoginMemberDto(1L, "user123", "test@test.com");
		WorkspaceMember adminWorkspaceMember = workspaceMemberEntityFixture.createAdminWorkspaceMember(member,
			workspace);

		when(memberRepository.findById(loginMember.getId())).thenReturn(Optional.of(member));
		when(workspaceCodeGenerator.generateWorkspaceCode()).thenReturn(code);
		when(workspaceRepository.save(any(Workspace.class))).thenReturn(workspace);
		when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(adminWorkspaceMember);

		// when
		workspaceCreateService.createWorkspace(request, loginMember.getId());

		// then
		verify(workspaceMemberRepository, times(1)).save(argThat(workspaceMember ->
			workspaceMember.getNickname().equals("test@test.com")
		));
	}

	@Test
	@DisplayName("워크스페이스 생성 시 WorkspaceMember의 권한은 ADMIN으로 설정된다")
	void test6() {
		// given
		WorkspaceCreateRequest request = workspaceCreateDtoFixture.createWorkspaceCreateRequest(null);
		String code = "TESTCODE";

		Workspace workspace = workspaceEntityFixture.createWorkspace("testcode");
		Member member = memberEntityFixture.createMember("user123", "test@test.com");
		LoginMember loginMember = loginMemberDtoFixture.createLoginMemberDto(1L, "user123", "test@test.com");
		WorkspaceMember adminWorkspaceMember = workspaceMemberEntityFixture.createAdminWorkspaceMember(member,
			workspace);

		when(memberRepository.findById(loginMember.getId())).thenReturn(Optional.of(member));
		when(workspaceCodeGenerator.generateWorkspaceCode()).thenReturn(code);
		when(workspaceRepository.save(any(Workspace.class))).thenReturn(workspace);
		when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(adminWorkspaceMember);

		// when
		workspaceCreateService.createWorkspace(request, loginMember.getId());

		// then
		verify(workspaceMemberRepository, times(1)).save(argThat(workspaceMember ->
			workspaceMember.getRole().equals(WorkspaceRole.ADMIN)
		));
	}
}
