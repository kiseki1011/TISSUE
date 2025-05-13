package com.tissue.integration.service.command;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.member.domain.Member;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.response.WorkspaceResponse;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.enums.WorkspaceRole;
import com.tissue.support.helper.ServiceIntegrationTestHelper;

class WorkspaceCreateServiceIT extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("워크스페이스를 생성할 수 있다")
	void canCreateWorkspace() {
		// given
		Member member = testDataFixture.createMember("member1");

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("test workspace")
			.description("test workspace")
			.build();

		// when
		WorkspaceResponse response = workspaceCreateService.createWorkspace(request, member.getId());

		// then
		Workspace workspace = workspaceRepository.findByCode(response.workspaceCode()).get();

		assertThat(response.workspaceCode()).isEqualTo(workspace.getCode());
	}

	@Test
	@DisplayName("생성된 워크스페이스는 8자리 BASE62 문자열로 이루어진 워크스페이스 코드를 가진다")
	void createdWorkspaceHasWorkspaceCode_Base62StringLength8() {
		// given
		Member member = testDataFixture.createMember("member1");

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("test workspace")
			.description("test workspace")
			.build();

		// when
		WorkspaceResponse response = workspaceCreateService.createWorkspace(request, member.getId());

		// then
		assertThat(response.workspaceCode().length()).isEqualTo(8);
	}

	@Test
	@Transactional
	@DisplayName("워크스페이스를 생성한 멤버는 OWNER 권한의 워크스페이스 멤버(WorkspaceMember)로 등록된다")
	void memberThatCreatedWorkspaceIsAddedAsOwnerWorkspaceMember() {
		// given
		Member member = testDataFixture.createMember("member1");

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("test workspace")
			.description("test workspace")
			.build();

		// when
		WorkspaceResponse response = workspaceCreateService.createWorkspace(request, member.getId());

		// then
		Workspace workspace = workspaceRepository.findByCode(response.workspaceCode()).get();
		assertThat(workspace.getWorkspaceMembers().stream().findFirst().get().getRole()).isEqualTo(WorkspaceRole.OWNER);

		WorkspaceMember workspaceMember = workspaceMemberRepository.findById(1L).get();
		assertThat(workspaceMember.getRole()).isEqualTo(WorkspaceRole.OWNER);
	}

	// Todo: 워크스페이스 멤버 별칭 방식 변경 후 수정
	@Test
	@Transactional
	@DisplayName("워크스페이스 생성 시 생성자인 워크스페이스 멤버(WorkspaceMember)의 별칭(displayName)이 기본적으로 설정된다")
	void workspaceCreate_WorkspaceMemberDefaultNicknameIsEmail() {
		// given
		Member member = testDataFixture.createMember("member1");

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("test workspace")
			.description("test workspace")
			.build();

		// when
		WorkspaceResponse response = workspaceCreateService.createWorkspace(request, member.getId());

		// then
		Workspace workspace = workspaceRepository.findByCode(response.workspaceCode()).get();
		assertThat(workspace.getWorkspaceMembers().stream().findFirst().get().getDisplayName())
			.isNotNull();

		WorkspaceMember workspaceMember = workspaceMemberRepository.findById(1L).get();
		assertThat(workspaceMember.getDisplayName()).isNotNull();
	}

	@Test
	@DisplayName("워크스페이스 생성 시, 생성한 멤버의 워크스페이스 카운트(myWorkspaceCount)가 증가한다")
	void memberThatCreatedWorkspace_GetsWorkspaceCountIncreased() {
		// given
		Member member = testDataFixture.createMember("member1");

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("test workspace")
			.description("test workspace")
			.build();

		// when
		workspaceCreateService.createWorkspace(request, member.getId());

		// then
		Member updatedMember = memberRepository.findById(member.getId()).get();
		assertThat(updatedMember.getMyWorkspaceCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("하나의 멤버가 OWNER로 가질수 있는 워크스페이스의 개수는 최대 10개이다")
	void maxNumberOfWorkspaces_MemberCanHaveIs_10() {
		// given
		Member member = testDataFixture.createMember("member1");

		// create 10 workspaces
		for (int i = 0; i < 10; i++) {
			CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
				.name("workspace" + i)
				.description("description" + i)
				.build();
			workspaceCreateService.createWorkspace(request, member.getId());
		}

		CreateWorkspaceRequest request11 = CreateWorkspaceRequest.builder()
			.name("workspace11")
			.description("description11")
			.build();

		// when & then - try 11th workspace create request
		assertThatThrownBy(() -> workspaceCreateService.createWorkspace(request11, member.getId()))
			.isInstanceOf(InvalidOperationException.class);
	}

	@Test
	@DisplayName("워크스페이스 생성 시, 이슈 키 접두사(issueKeyPrefix)의 값을 제공하지 않으면 기본적으로 'ISSUE'로 설정된다")
	void whenCreatingWorkspace_IfNoIssueKeyPrefixProvided_SetToDefaultPrefix_ISSUE() {
		// given
		Member member = testDataFixture.createMember("member1");

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("test workspace")
			.description("test workspace")
			.build();

		// when
		WorkspaceResponse response = workspaceCreateService.createWorkspace(request, member.getId());

		// then
		Workspace workspace = workspaceRepository.findByCode(response.workspaceCode()).get();

		assertThat(workspace.getIssueKeyPrefix()).isEqualTo("ISSUE");
	}

	@Test
	@DisplayName("워크스페이스 생성 시, 제공한 이슈 키 접두사(issueKeyPrefix)의 값으로 설정된다")
	void whenCreatingWorkspace_IfKeyPrefixProvided_SetToProvidedPrefix() {
		// given
		Member member = testDataFixture.createMember("member1");

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("test workspace")
			.description("test workspace")
			.issueKeyPrefix("TESTPREFIX") // provide prefix as "TESTPREFIX"
			.build();

		// when
		WorkspaceResponse response = workspaceCreateService.createWorkspace(request, member.getId());

		// then
		Workspace workspace = workspaceRepository.findByCode(response.workspaceCode()).get();

		assertThat(workspace.getIssueKeyPrefix()).isEqualTo("TESTPREFIX");
	}
}
