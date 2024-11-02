package com.uranus.taskmanager.api.workspace.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.security.PasswordEncoder;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceContentUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceDeleteRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspacePasswordUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceContentUpdateResponse;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.fixture.repository.MemberRespositoryFixture;
import com.uranus.taskmanager.fixture.repository.WorkspaceRepositoryFixture;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
public class WorkspaceCommandServiceTest {

	@Autowired
	private WorkspaceService workspaceService;
	@Autowired
	private WorkspaceCommandService workspaceCommandService;
	@Autowired
	private WorkspaceRepository workspaceRepository;
	@Autowired
	private MemberRepository memberRepository;
	@Autowired
	private WorkspaceMemberRepository workspaceMemberRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private EntityManager entityManager;

	@Autowired
	private WorkspaceRepositoryFixture workspaceRepositoryFixture;
	@Autowired
	private MemberRespositoryFixture memberRespositoryFixture;

	@AfterEach
	void tearDown() {
		workspaceMemberRepository.deleteAll();
		workspaceRepository.deleteAll();
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("유효한 워크스페이스 코드와 비밀번호로 워크스페이스를 삭제할 수 있다")
	void test1() {
		// given
		Member member = memberRespositoryFixture.createMember("member1", "member1@test.com", "member1password!");
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			passwordEncoder.encode("password1234!"));
		workspaceRepositoryFixture.addMemberToWorkspace(member, workspace, WorkspaceRole.ADMIN);

		workspaceRepositoryFixture.createWorkspace("workspace2", "description2", "TEST2222", null);

		// when
		workspaceCommandService.deleteWorkspace(new WorkspaceDeleteRequest("password1234!"), "TEST1111");

		// then
		assertThat(workspaceRepository.findByCode("TEST1111")).isEmpty();
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 삭제 시도 시 비밀번호가 맞지 않으면 예외가 발생한다")
	void test2() {
		// given
		Member member = memberRespositoryFixture.createMember("member1", "member1@test.com", "member1password!");
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			passwordEncoder.encode("password1234!"));
		workspaceRepositoryFixture.addMemberToWorkspace(member, workspace, WorkspaceRole.ADMIN);

		// when & then
		assertThatThrownBy(
			() -> workspaceCommandService.deleteWorkspace(new WorkspaceDeleteRequest("InvalidPassword"), "TEST1111"))
			.isInstanceOf(InvalidWorkspacePasswordException.class);

	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 삭제 시도 시 코드가 유효하지 않으면 예외가 발생한다")
	void test3() {
		// given
		Member member = memberRespositoryFixture.createMember("member1", "member1@test.com", "member1password!");
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			passwordEncoder.encode("password1234!"));
		workspaceRepositoryFixture.addMemberToWorkspace(member, workspace, WorkspaceRole.ADMIN);

		// when & then
		assertThatThrownBy(
			() -> workspaceCommandService.deleteWorkspace(new WorkspaceDeleteRequest("password1234!"), "INVALIDCODE"))
			.isInstanceOf(WorkspaceNotFoundException.class);

	}

	@Transactional
	@Test
	@DisplayName("유효한 워크스페이스 코드로 워크스페이스의 이름과 설명을 수정할 수 있다")
	void test4() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			null);

		WorkspaceContentUpdateRequest request = WorkspaceContentUpdateRequest.builder()
			.name("Updated Name")
			.description("Updated Description")
			.build();

		// when
		WorkspaceContentUpdateResponse response = workspaceCommandService.updateWorkspaceContent(request, "TEST1111");

		// then
		assertThat(response.getUpdatedTo().getName()).isEqualTo("Updated Name");
		assertThat(response.getUpdatedTo().getDescription()).isEqualTo("Updated Description");
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스의 이름만 수정하면 해당 필드만 업데이트된다")
	void test5() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			null);

		WorkspaceContentUpdateRequest request = WorkspaceContentUpdateRequest.builder()
			.name("Updated Name")
			.build();

		// when
		WorkspaceContentUpdateResponse response = workspaceCommandService.updateWorkspaceContent(request, "TEST1111");

		// then
		assertThat(response.getUpdatedTo().getName()).isEqualTo("Updated Name");
		assertThat(response.getUpdatedTo().getDescription()).isEqualTo("description1");
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스의 설명만 수정하면 해당 필드만 업데이트된다")
	void test6() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			null);

		WorkspaceContentUpdateRequest request = WorkspaceContentUpdateRequest.builder()
			.description("Updated Description")
			.build();

		// when
		WorkspaceContentUpdateResponse response = workspaceCommandService.updateWorkspaceContent(request, "TEST1111");

		// then
		assertThat(response.getUpdatedTo().getName()).isEqualTo("workspace1");
		assertThat(response.getUpdatedTo().getDescription()).isEqualTo("Updated Description");
	}

	@Transactional
	@Test
	@DisplayName("비밀번호 수정 요청의 원본 비밀번호가 유효하면 요청의 수정 비밀번호로 업데이트된다")
	void test7() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			passwordEncoder.encode("password1234!"));

		WorkspacePasswordUpdateRequest request = WorkspacePasswordUpdateRequest.builder()
			.originalPassword("password1234!")
			.updatePassword("updated1234!")
			.build();

		// when
		workspaceCommandService.updateWorkspacePassword(request, "TEST1111");
		entityManager.flush();

		// then
		String updatedPassword = workspaceRepository.findByCode("TEST1111").get().getPassword();
		assertThat(passwordEncoder.matches("updated1234!", updatedPassword)).isTrue();
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스의 비밀번호가 null이면 비밀번호 수정 요청의 수정 비밀번호로 업데이트된다")
	void test8() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			null);

		WorkspacePasswordUpdateRequest request = WorkspacePasswordUpdateRequest.builder()
			.updatePassword("updated1234!")
			.build();

		// when
		workspaceCommandService.updateWorkspacePassword(request, "TEST1111");
		entityManager.flush();

		// then
		String updatedPassword = workspaceRepository.findByCode("TEST1111").get().getPassword();
		assertThat(passwordEncoder.matches("updated1234!", updatedPassword)).isTrue();
	}

	@Test
	@DisplayName("비밀번호 수정 요청의 원본 비밀번호가 유효하지 않으면 예외가 발생한다")
	void test9() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			passwordEncoder.encode("password1234!"));

		WorkspacePasswordUpdateRequest request = WorkspacePasswordUpdateRequest.builder()
			.originalPassword("invalid1234!")
			.updatePassword("updated1234!")
			.build();

		// when
		assertThatThrownBy(() -> workspaceCommandService.updateWorkspacePassword(request, "TEST1111"))
			.isInstanceOf(InvalidWorkspacePasswordException.class);
	}

	@Transactional
	@Test
	@DisplayName("비밀번호 수정 요청의 수정 비밀번호를 제공하지 않으면 비밀번호는 null로 업데이트 된다")
	void test10() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			passwordEncoder.encode("password1234!"));

		WorkspacePasswordUpdateRequest request = WorkspacePasswordUpdateRequest.builder()
			.originalPassword("password1234!")
			.build();

		// when
		workspaceCommandService.updateWorkspacePassword(request, "TEST1111");
		entityManager.flush();

		// then
		String updatedPassword = workspaceRepository.findByCode("TEST1111").get().getPassword();
		assertThat(updatedPassword).isNull();

	}

	@Transactional
	@Test
	@DisplayName("비밀번호 수정 요청의 수정 비밀번호를 null로 제공하면 비밀번호는 null로 업데이트 된다")
	void test11() {
		// given
		Workspace workspace = workspaceRepositoryFixture.createWorkspace("workspace1", "description1", "TEST1111",
			passwordEncoder.encode("password1234!"));

		WorkspacePasswordUpdateRequest request = WorkspacePasswordUpdateRequest.builder()
			.originalPassword("password1234!")
			.updatePassword(null)
			.build();

		// when
		workspaceCommandService.updateWorkspacePassword(request, "TEST1111");
		entityManager.flush();

		// then
		String updatedPassword = workspaceRepository.findByCode("TEST1111").get().getPassword();
		assertThat(updatedPassword).isNull();

	}
}
