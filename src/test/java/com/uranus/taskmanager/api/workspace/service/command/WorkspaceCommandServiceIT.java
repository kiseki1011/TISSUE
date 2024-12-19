package com.uranus.taskmanager.api.workspace.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspace.exception.WorkspaceNotFoundException;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.DeleteWorkspaceRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.UpdateWorkspaceInfoRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.request.UpdateWorkspacePasswordRequest;
import com.uranus.taskmanager.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.uranus.taskmanager.api.workspace.presentation.dto.response.UpdateWorkspaceInfoResponse;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class WorkspaceCommandServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("유효한 워크스페이스 코드와 비밀번호로 워크스페이스를 삭제할 수 있다")
	void test1() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember("member1", "member1@test.com", "member1password!");

		CreateWorkspaceResponse response = workspaceCreateService.createWorkspace(CreateWorkspaceRequest.builder()
			.name("workspace1")
			.description("description1")
			.build(), member.getId());

		// when
		workspaceCommandService.deleteWorkspace(new DeleteWorkspaceRequest(), response.code(),
			member.getId());

		// then
		assertThat(workspaceRepository.findByCode(response.code())).isEmpty();
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 삭제 시도 시 비밀번호가 맞지 않으면 예외가 발생한다")
	void test2() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember("member1", "member1@test.com", "member1password!");

		CreateWorkspaceResponse response = workspaceCreateService.createWorkspace(CreateWorkspaceRequest.builder()
			.name("workspace1")
			.description("description1")
			.password("password1234!")
			.build(), member.getId());

		// when & then
		assertThatThrownBy(
			() -> workspaceCommandService.deleteWorkspace(new DeleteWorkspaceRequest("InvalidPassword"),
				response.code(),
				member.getId()))
			.isInstanceOf(InvalidWorkspacePasswordException.class);

	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 삭제 시도 시 코드가 유효하지 않으면 예외가 발생한다")
	void test3() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember("member1", "member1@test.com", "member1password!");
		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace("workspace1", "description1",
			"TEST1111",
			"password1234!");
		workspaceRepositoryFixture.addAndSaveMemberToWorkspace(member, workspace, WorkspaceRole.MANAGER);

		// when & then
		assertThatThrownBy(
			() -> workspaceCommandService.deleteWorkspace(new DeleteWorkspaceRequest("password1234!"), "INVALIDCODE",
				member.getId()))
			.isInstanceOf(WorkspaceNotFoundException.class);

	}

	@Transactional
	@Test
	@DisplayName("유효한 워크스페이스 코드로 워크스페이스의 이름과 설명을 수정할 수 있다")
	void test4() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace("workspace1", "description1", "TEST1111", null);

		UpdateWorkspaceInfoRequest request = UpdateWorkspaceInfoRequest.builder()
			.name("Updated Name")
			.description("Updated Description")
			.build();

		// when
		UpdateWorkspaceInfoResponse response = workspaceCommandService.updateWorkspaceInfo(request, "TEST1111");

		// then
		assertThat(response.code()).isEqualTo("TEST1111");

		assertThat(workspaceRepository.findByCode("TEST1111")
			.orElseThrow()
			.getName()
		)
			.isEqualTo("Updated Name");
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스의 이름만 수정하면 해당 필드만 업데이트된다")
	void test5() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace("workspace1", "description1", "TEST1111", null);

		UpdateWorkspaceInfoRequest request = UpdateWorkspaceInfoRequest.builder()
			.name("Updated Name")
			.build();

		// when
		workspaceCommandService.updateWorkspaceInfo(request, "TEST1111");

		// then
		assertThat(workspaceRepository.findByCode("TEST1111")
			.orElseThrow()
			.getName()
		)
			.isEqualTo("Updated Name");

		assertThat(workspaceRepository.findByCode("TEST1111")
			.orElseThrow()
			.getDescription()
		)
			.isEqualTo("description1");
	}

	@Transactional
	@Test
	@DisplayName("비밀번호 수정 요청의 원본 비밀번호가 유효하면 요청의 수정 비밀번호로 업데이트된다")
	void test7() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace("workspace1", "description1", "TEST1111", "password1234!");

		UpdateWorkspacePasswordRequest request = UpdateWorkspacePasswordRequest.builder()
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
		workspaceRepositoryFixture.createAndSaveWorkspace("workspace1", "description1", "TEST1111", null);

		UpdateWorkspacePasswordRequest request = UpdateWorkspacePasswordRequest.builder()
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
		workspaceRepositoryFixture.createAndSaveWorkspace("workspace1", "description1", "TEST1111", "password1234!");

		UpdateWorkspacePasswordRequest request = UpdateWorkspacePasswordRequest.builder()
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
		workspaceRepositoryFixture.createAndSaveWorkspace("workspace1", "description1", "TEST1111", "password1234!");

		UpdateWorkspacePasswordRequest request = UpdateWorkspacePasswordRequest.builder()
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
		workspaceRepositoryFixture.createAndSaveWorkspace("workspace1", "description1", "TEST1111", "password1234!");

		UpdateWorkspacePasswordRequest request = UpdateWorkspacePasswordRequest.builder()
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
