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

import com.uranus.taskmanager.api.auth.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceResponse;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.util.WorkspaceCodeGenerator;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.fixture.MockFixture;

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

	MockFixture mockFixture;

	@BeforeEach
	public void setup() {
		mockFixture = new MockFixture();
	}

	@Test
	@DisplayName("워크스페이스 생성에는 생성 요청과 로그인 멤버를 필요로 한다")
	void test1() {
		// given
		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("test name")
			.description("test description")
			.build();

		Workspace mockWorkspace = mockFixture.mockWorkspace("testcode");
		Member mockMember = mockFixture.mockMember("user123", "test@test.com");
		LoginMemberDto mockLoginMember = mockFixture.mockLoginMember("user123", "test@test.com");

		when(memberRepository.findByLoginId(mockLoginMember.getLoginId())).thenReturn(Optional.of(mockMember));
		when(workspaceRepository.save(any(Workspace.class))).thenReturn(mockWorkspace);

		// when
		WorkspaceResponse response = workspaceCreateService.createWorkspace(request, mockLoginMember);

		// then
		assertThat(response).isNotNull();
		verify(memberRepository, times(1)).findByLoginId("user123");
	}

	@Test
	@DisplayName("워크스페이스 생성을 성공하면 WorkspaceResponse를 반환한다")
	void test2() {
		// given
		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("test name")
			.description("test description")
			.build();

		Workspace mockWorkspace = mockFixture.mockWorkspace("testcode");
		Member mockMember = mockFixture.mockMember("user123", "test@test.com");
		LoginMemberDto mockLoginMember = mockFixture.mockLoginMember("user123", "test@test.com");

		when(memberRepository.findByLoginId(mockLoginMember.getLoginId())).thenReturn(Optional.of(mockMember));
		when(workspaceRepository.save(any(Workspace.class))).thenReturn(mockWorkspace);

		// when
		WorkspaceResponse response = workspaceCreateService.createWorkspace(request, mockLoginMember);

		// then
		assertThat(response).isNotNull();
		assertThat(response.getName()).isEqualTo("test name");
		assertThat(response.getDescription()).isEqualTo("test description");
		assertThat(response.getCode()).isEqualTo("testcode");
		verify(workspaceRepository, times(1)).save(any(Workspace.class));
	}

	@Test
	@DisplayName("워크스페이스 코드가 중복될 때 최대 재시도 횟수(5회)를 소진하면 예외가 발생한다")
	void test3() {
		// given
		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("test name")
			.description("test description")
			.build();

		Member mockMember = mockFixture.mockMember("user123", "test@test.com");
		LoginMemberDto mockLoginMember = mockFixture.mockLoginMember("user123", "test@test.com");
		when(memberRepository.findByLoginId(mockLoginMember.getLoginId())).thenReturn(Optional.of(mockMember));

		when(workspaceCodeGenerator.generateWorkspaceCode())
			.thenReturn("WORK123", "WORK124", "WORK125", "WORK126", "WORK127");
		when(workspaceRepository.existsByCode(anyString())).thenReturn(true);

		// when & then
		assertThatThrownBy(() -> workspaceCreateService.createWorkspace(request, mockLoginMember))
			.isInstanceOf(RuntimeException.class)  // Todo: WorkspaceCodeCollisionHandleException 구현 후 수정
			.hasMessageContaining("Failed to solve workspace code collision");
	}

	@Test
	@DisplayName("워크스페이스 생성 시 WorkspaceMember도 생성되고 저장된다")
	void test4() {
		// given
		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("test name")
			.description("test description")
			.build();

		Workspace mockWorkspace = mockFixture.mockWorkspace("testcode");
		Member mockMember = mockFixture.mockMember("user123", "test@test.com");
		LoginMemberDto mockLoginMember = mockFixture.mockLoginMember("user123", "test@test.com");
		WorkspaceMember mockWorkspaceMember = mockFixture.mockAdminWorkspaceMember(mockMember, mockWorkspace);
		when(memberRepository.findByLoginId(mockLoginMember.getLoginId())).thenReturn(Optional.of(mockMember));
		when(workspaceRepository.save(any(Workspace.class))).thenReturn(mockWorkspace);
		when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(mockWorkspaceMember);

		// when
		workspaceCreateService.createWorkspace(request, mockLoginMember);

		// then
		verify(workspaceMemberRepository, times(1)).save(any(WorkspaceMember.class));
	}

	@Test
	@DisplayName("워크스페이스 생성 시 WorkspaceMember의 별칭은 생성자의 이메일로 설정된다")
	void test5() {
		// given
		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("test name")
			.description("test description")
			.build();

		Workspace mockWorkspace = mockFixture.mockWorkspace("testcode");
		Member mockMember = mockFixture.mockMember("user123", "test@test.com");
		LoginMemberDto mockLoginMember = mockFixture.mockLoginMember("user123", "test@test.com");
		WorkspaceMember mockWorkspaceMember = mockFixture.mockAdminWorkspaceMember(mockMember, mockWorkspace);

		when(memberRepository.findByLoginId(mockLoginMember.getLoginId())).thenReturn(Optional.of(mockMember));
		when(workspaceRepository.save(any(Workspace.class))).thenReturn(mockWorkspace);
		when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(mockWorkspaceMember);

		// when
		workspaceCreateService.createWorkspace(request, mockLoginMember);

		// then
		verify(workspaceMemberRepository, times(1)).save(argThat(workspaceMember ->
			workspaceMember.getNickname().equals("test@test.com")
		));
	}

	@Test
	@DisplayName("워크스페이스 생성 시 WorkspaceMember의 권한은 ADMIN으로 설정된다")
	void test6() {
		// given
		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("test name")
			.description("test description")
			.build();

		Workspace mockWorkspace = mockFixture.mockWorkspace("testcode");
		Member mockMember = mockFixture.mockMember("user123", "test@test.com");
		LoginMemberDto mockLoginMember = mockFixture.mockLoginMember("user123", "test@test.com");
		WorkspaceMember mockWorkspaceMember = mockFixture.mockAdminWorkspaceMember(mockMember, mockWorkspace);

		when(memberRepository.findByLoginId(mockLoginMember.getLoginId())).thenReturn(Optional.of(mockMember));
		when(workspaceRepository.save(any(Workspace.class))).thenReturn(mockWorkspace);
		when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(mockWorkspaceMember);

		// when
		workspaceCreateService.createWorkspace(request, mockLoginMember);

		// then
		verify(workspaceMemberRepository, times(1)).save(argThat(workspaceMember ->
			workspaceMember.getRole().equals(WorkspaceRole.ADMIN)
		));
	}
}
