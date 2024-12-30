package com.tissue.api.workspace.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateIssueKeyRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspacePasswordRequest;
import com.tissue.api.workspace.presentation.dto.response.CreateWorkspaceResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateIssueKeyResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateWorkspaceInfoResponse;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.helper.ServiceIntegrationTestHelper;

class WorkspaceCommandServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("유효한 워크스페이스 코드로 워크스페이스를 삭제할 수 있다")
	void test1() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"member1password!"
		);

		CreateWorkspaceResponse response = workspaceCreateService.createWorkspace(CreateWorkspaceRequest.builder()
			.name("workspace1")
			.description("description1")
			.build(), member.getId());

		// when
		workspaceCommandService.deleteWorkspace(response.code(), member.getId());

		// then
		assertThat(workspaceRepository.findByCode(response.code())).isEmpty();
	}

	@Transactional
	@Test
	@DisplayName("워크스페이스 삭제 시도 시 코드가 유효하지 않으면 예외가 발생한다")
	void test3() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"member1password!"
		);

		Workspace workspace = workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TEST1111",
			"password1234!"
		);
		workspaceRepositoryFixture.addAndSaveMemberToWorkspace(member, workspace, WorkspaceRole.MANAGER);

		// when & then
		assertThatThrownBy(() -> workspaceCommandService.deleteWorkspace("INVALIDCODE", member.getId()))
			.isInstanceOf(WorkspaceNotFoundException.class);

	}

	@Transactional
	@Test
	@DisplayName("유효한 워크스페이스 코드로 워크스페이스의 이름과 설명을 수정할 수 있다")
	void test4() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TEST1111",
			null
		);

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
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TEST1111",
			null
		);

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
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TEST1111",
			"password1234!"
		);

		UpdateWorkspacePasswordRequest request = new UpdateWorkspacePasswordRequest("password1234!", "updated1234!");

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
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TEST1111",
			null
		);

		UpdateWorkspacePasswordRequest request = new UpdateWorkspacePasswordRequest(null, "updated1234!");

		// when
		workspaceCommandService.updateWorkspacePassword(request, "TEST1111");
		entityManager.flush();

		// then
		String updatedPassword = workspaceRepository.findByCode("TEST1111").get().getPassword();
		assertThat(passwordEncoder.matches("updated1234!", updatedPassword)).isTrue();
	}

	@Transactional
	@Test
	@DisplayName("비밀번호 수정 요청의 수정 비밀번호를 null로 제공하면 비밀번호는 null로 업데이트 된다")
	void test11() {
		// given
		workspaceRepositoryFixture.createAndSaveWorkspace(
			"workspace1",
			"description1",
			"TEST1111",
			"password1234!"
		);

		UpdateWorkspacePasswordRequest request = new UpdateWorkspacePasswordRequest("password1234!", null);

		// when
		workspaceCommandService.updateWorkspacePassword(request, "TEST1111");
		entityManager.flush();

		// then
		String updatedPassword = workspaceRepository.findByCode("TEST1111").get().getPassword();
		assertThat(updatedPassword).isNull();
	}

	@Test
	@DisplayName("key prefix를 요청을 통해 제공한 key prefix로 업데이트 할 수 있다")
	void testUpdateKeyPrefix() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		CreateWorkspaceRequest createRequest = CreateWorkspaceRequest.builder()
			.name("workspace1")
			.description("description1")
			.build();

		workspaceCreateService.createWorkspace(createRequest, member.getId());

		UpdateIssueKeyRequest request = new UpdateIssueKeyRequest("UPDATEPREFIX");

		// when
		Workspace workspace = workspaceRepository.findById(1L).orElseThrow();
		UpdateIssueKeyResponse response = workspaceCommandService.updateIssueKey(workspace.getCode(), request);

		// then
		assertThat(response.keyPrefix()).isEqualTo("UPDATEPREFIX");
	}

	@Test
	@DisplayName("워크스페이스 삭제 시 멤버의 워크스페이스 카운트가 감소한다")
	void deleteWorkspace_decreasesWorkspaceCount() {
		// given
		Member member = memberRepositoryFixture.createAndSaveMember(
			"member1",
			"member1@test.com",
			"password1234!"
		);

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("workspace1")
			.description("description1")
			.build();

		CreateWorkspaceResponse response = workspaceCreateService.createWorkspace(request, member.getId());

		// when
		workspaceCommandService.deleteWorkspace(response.code(), member.getId());

		// then
		Member updatedMember = memberRepository.findById(member.getId()).get();
		assertThat(updatedMember.getMyWorkspaceCount()).isEqualTo(0);
	}
}
