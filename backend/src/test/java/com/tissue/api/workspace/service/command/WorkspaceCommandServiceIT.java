package com.tissue.api.workspace.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;
import com.tissue.api.workspace.presentation.dto.request.UpdateIssueKeyRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspacePasswordRequest;
import com.tissue.api.workspace.presentation.dto.response.UpdateIssueKeyResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateWorkspaceInfoResponse;
import com.tissue.helper.ServiceIntegrationTestHelper;

class WorkspaceCommandServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@Transactional
	@DisplayName("유효한 워크스페이스 코드로 워크스페이스를 삭제할 수 있다")
	void canDeleteWorkspaceWithValidWorkspaceCode() {
		// given
		Member member = testDataFixture.createMember("member1");

		// Todo: mocking 고려
		Workspace workspace = testDataFixture.createWorkspace("test workspace", null, null);
		member.increaseMyWorkspaceCount(); // increase workspace count of member

		// when
		workspaceCommandService.deleteWorkspace(workspace.getCode(), member.getId());

		// then
		assertThat(workspaceRepository.findByCode(workspace.getCode())).isEmpty();
	}

	@Test
	@Transactional
	@DisplayName("유효하지 않은 코드로 워크스페이스를 삭제할 수 없다")
	void cannotDeleteWorkspaceWithInvalidWorkspaceCode() {
		// given
		Member member = testDataFixture.createMember("member1");
		Workspace workspace = testDataFixture.createWorkspace("test workspace", null, null);

		// when & then
		assertThatThrownBy(() -> workspaceCommandService.deleteWorkspace("INVALIDCODE", member.getId()))
			.isInstanceOf(WorkspaceNotFoundException.class);

	}

	@Test
	@Transactional
	@DisplayName("유효한 워크스페이스 코드로 워크스페이스의 이름과 설명을 업데이트할 수 있다")
	void canUpdateWorkspaceNameAndDescriptionWithValidWorkspaceCode() {
		// given
		Workspace workspace = testDataFixture.createWorkspace(
			"test workspace",
			null,
			null
		);

		UpdateWorkspaceInfoRequest request = UpdateWorkspaceInfoRequest.builder()
			.name("updated workspace name")
			.description("updated workspace description")
			.build();

		// when
		UpdateWorkspaceInfoResponse response = workspaceCommandService.updateWorkspaceInfo(
			request,
			workspace.getCode()
		);

		// then
		assertThat(response.code()).isEqualTo(workspace.getCode());
		assertThat(workspaceRepository.findByCode(workspace.getCode()).get().getName())
			.isEqualTo("updated workspace name");
	}

	@Test
	@Transactional
	@DisplayName("워크스페이스의 이름만 수정하면 해당 필드만 업데이트된다")
	void onlyWorkspaceNameIsUpdatedIfOnlyNameWasProvided() {
		// given
		Workspace workspace = testDataFixture.createWorkspace(
			"test workspace",
			null,
			null
		); // description: "description"

		UpdateWorkspaceInfoRequest request = UpdateWorkspaceInfoRequest.builder()
			.name("updated workspace name")
			.build();

		// when
		workspaceCommandService.updateWorkspaceInfo(request, workspace.getCode());

		// then
		assertThat(workspaceRepository.findByCode(workspace.getCode()).get().getName())
			.isEqualTo("updated workspace name");

		assertThat(workspaceRepository.findByCode(workspace.getCode()).get().getDescription())
			.isEqualTo("description");
	}

	@Test
	@Transactional
	@DisplayName("유효한 원본 패스워드로 워크스페이스의 패스워드를 업데이트할 수 있다")
	void canUpdateWorkspacePasswordIfOriginalPasswordIsValid() {
		// given
		Workspace workspace = testDataFixture.createWorkspace(
			"test workspace",
			"password1234!",
			null
		);

		UpdateWorkspacePasswordRequest request = new UpdateWorkspacePasswordRequest("test1234!", "updated1234!");

		// when
		workspaceCommandService.updateWorkspacePassword(request, workspace.getCode());
		entityManager.flush();

		// then
		String updatedPassword = workspaceRepository.findByCode(workspace.getCode()).get().getPassword();

		assertThat(passwordEncoder.matches("updated1234!", updatedPassword)).isTrue();
	}

	@Test
	@Transactional
	@DisplayName("워크스페이스의 패스워드를 설정하지 않은 경우 패스워드 제공 없이 패스워드 업데이트가 가능하다")
	void canUpdateWorkspacePasswordWithoutOriginalPasswordIfWorkspaceDidNotHavePassword() {
		// given
		Workspace workspace = testDataFixture.createWorkspace(
			"test workspace",
			null,
			null
		);

		UpdateWorkspacePasswordRequest request = new UpdateWorkspacePasswordRequest(null, "updated1234!");

		// when
		workspaceCommandService.updateWorkspacePassword(request, workspace.getCode());
		entityManager.flush();

		// then
		String updatedPassword = workspaceRepository.findByCode(workspace.getCode()).get().getPassword();

		assertThat(passwordEncoder.matches("updated1234!", updatedPassword)).isTrue();
	}

	@Test
	@Transactional
	@DisplayName("워크스페이스 패스워드를 null로 업데이트 할 수 있다")
	void canUpdateWorkspaceToNotHavePassword() {
		// given
		Workspace workspace = testDataFixture.createWorkspace(
			"test workspace",
			null,
			null
		);

		UpdateWorkspacePasswordRequest request = new UpdateWorkspacePasswordRequest("password1234!", null);

		// when
		workspaceCommandService.updateWorkspacePassword(request, workspace.getCode());
		entityManager.flush();

		// then
		String updatedPassword = workspaceRepository.findByCode(workspace.getCode()).get().getPassword();
		assertThat(updatedPassword).isNull();
	}

	@Test
	@Transactional
	@DisplayName("워크스페이스 마다 관리하는 이슈 키 접두사(issueKeyPrefix)를 업데이트 할 수 있다")
	void canUpdateIssueKeyPrefix() {
		// given
		Member member = testDataFixture.createMember("member1");

		// default prefix: "ISSUE"
		Workspace workspace = testDataFixture.createWorkspace("test workspace", null, null);

		UpdateIssueKeyRequest request = new UpdateIssueKeyRequest("UPDATEPREFIX");

		// when
		UpdateIssueKeyResponse response = workspaceCommandService.updateIssueKey(workspace.getCode(), request);

		// then
		assertThat(response.keyPrefix()).isEqualTo("UPDATEPREFIX");
	}

	@Test
	@Transactional
	@DisplayName("워크스페이스 삭제 시 해당 워크스페이스의 OWNER의 워크스페이스 카운트가 감소한다")
	void deleteWorkspaceDecreasesOwnersWorkspaceCount() {
		// given
		Member member = testDataFixture.createMember("member1");

		// Todo: mocking 고려
		Workspace workspace = testDataFixture.createWorkspace("test workspace", null, null);
		member.increaseMyWorkspaceCount();

		int memberWorkspaceCount = member.getMyWorkspaceCount();

		// when
		workspaceCommandService.deleteWorkspace(workspace.getCode(), member.getId());

		// then
		assertThat(memberWorkspaceCount).isEqualTo(1);

		Member updatedMember = memberRepository.findById(member.getId()).get();
		assertThat(updatedMember.getMyWorkspaceCount()).isEqualTo(0);
	}
}
