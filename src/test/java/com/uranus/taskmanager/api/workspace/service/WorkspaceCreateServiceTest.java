package com.uranus.taskmanager.api.workspace.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.WorkspaceCreationLimitExceededException;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceDeleteRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceCreateResponse;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class WorkspaceCreateServiceTest extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
	}

	@Test
	@DisplayName("워크스페이스 생성을 성공하면 워크스페이스 생성 응답을 반환한다")
	void createWorkspace_returnsWorkspaceCreateResponse() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("member1")
			.email("member1@test.com")
			.password(passwordEncoder.encode("password1234!"))
			.build());

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("workspace1")
			.description("description1")
			.build();

		// when
		WorkspaceCreateResponse response = workspaceCreateService.createWorkspace(request, member.getId());

		// then
		assertThat(response.getName()).isEqualTo("workspace1");
		assertThat(response.getDescription()).isEqualTo("description1");
	}

	@Test
	@DisplayName("워크스페이스 생성 시 OWNER 권한의 WorkspaceMember도 생성되고 저장된다")
	void workspaceCreate_ownerWorkspaceMemberIsSaved() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("member1")
			.email("member1@test.com")
			.password(passwordEncoder.encode("password1234!"))
			.build());

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("workspace1")
			.description("description1")
			.build();

		// when
		workspaceCreateService.createWorkspace(request, member.getId());

		// then
		WorkspaceMember workspaceMember = workspaceMemberRepository.findById(1L).get();
		assertThat(workspaceMember.getRole()).isEqualTo(WorkspaceRole.OWNER);
	}

	@Test
	@DisplayName("워크스페이스 생성 시 WorkspaceMember의 별칭은 생성자의 이메일로 설정된다")
	void workspaceCreate_workspaceMemberNicknameMustBeEmail() {
		// given
		Member member = memberRepository.save(Member.builder()
			.loginId("member1")
			.email("member1@test.com")
			.password(passwordEncoder.encode("password1234!"))
			.build());

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("workspace1")
			.description("description1")
			.build();

		// when
		workspaceCreateService.createWorkspace(request, member.getId());

		// then
		WorkspaceMember workspaceMember = workspaceMemberRepository.findById(1L).get();
		assertThat(workspaceMember.getNickname()).isEqualTo(member.getEmail());
	}

	@Test
	@DisplayName("워크스페이스 생성 시 멤버의 워크스페이스 카운트가 증가한다")
	void createWorkspace_increasesWorkspaceCount() {
		// given
		Member member = memberRepository.save(
			Member.builder()
				.loginId("member1")
				.email("member1@test.com")
				.password("password1234!")
				.build()
		);

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("workspace1")
			.description("description1")
			.build();

		// when
		workspaceCreateService.createWorkspace(request, member.getId());

		// then
		Member updatedMember = memberRepository.findById(member.getId()).get();
		assertThat(updatedMember.getWorkspaceCount()).isEqualTo(1);
	}

	@Test
	@DisplayName("워크스페이스 생성 시 멤버의 워크스페이스 카운트가 증가한다(워크스페이스 2개 생성)")
	void createTwoWorkspaces_increasesWorkspaceCount() {
		// given
		Member member = memberRepository.save(
			Member.builder()
				.loginId("member1")
				.email("member1@test.com")
				.password("password1234!")
				.build()
		);

		WorkspaceCreateRequest request1 = WorkspaceCreateRequest.builder()
			.name("workspace1")
			.description("description1")
			.build();

		WorkspaceCreateRequest request2 = WorkspaceCreateRequest.builder()
			.name("workspace2")
			.description("description2")
			.build();

		// when
		workspaceCreateService.createWorkspace(request1, member.getId());
		workspaceCreateService.createWorkspace(request2, member.getId());

		// then
		Member updatedMember = memberRepository.findById(member.getId()).get();
		assertThat(updatedMember.getWorkspaceCount()).isEqualTo(2);
	}

	@Test
	@DisplayName("워크스페이스 50개 생성 후 추가 생성 시 예외가 발생한다")
	void createWorkspace_throwsException_whenLimitReached() {
		// given
		Member member = memberRepository.save(
			Member.builder()
				.loginId("member1")
				.email("member1@test.com")
				.password("password1")
				.build()
		);

		// Create 50 workspaces
		for (int i = 0; i < 50; i++) {
			WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
				.name("workspace" + i)
				.description("description" + i)
				.build();
			workspaceCreateService.createWorkspace(request, member.getId());
		}

		// when & then
		WorkspaceCreateRequest request51 = WorkspaceCreateRequest.builder()
			.name("workspace51")
			.description("description51")
			.build();

		assertThatThrownBy(() -> workspaceCreateService.createWorkspace(request51, member.getId()))
			.isInstanceOf(WorkspaceCreationLimitExceededException.class);
	}

	@Test
	@DisplayName("워크스페이스 삭제 시 멤버의 워크스페이스 카운트가 감소한다")
	void deleteWorkspace_decreasesWorkspaceCount() {
		// given
		Member member = memberRepository.save(
			Member.builder()
				.loginId("member1")
				.email("member1@test.com")
				.password("password1234!")
				.build()
		);

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("workspace1")
			.description("description1")
			.build();

		WorkspaceCreateResponse response = workspaceCreateService.createWorkspace(request, member.getId());

		// when
		workspaceCommandService.deleteWorkspace(new WorkspaceDeleteRequest(), response.getCode(), member.getId());

		// then
		Member updatedMember = memberRepository.findById(member.getId()).get();
		assertThat(updatedMember.getWorkspaceCount()).isEqualTo(0);
	}
}
