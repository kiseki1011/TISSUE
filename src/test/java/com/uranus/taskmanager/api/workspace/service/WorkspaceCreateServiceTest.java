package com.uranus.taskmanager.api.workspace.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.WorkspaceCreationLimitExceededException;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceDeleteRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceCreateResponse;
import com.uranus.taskmanager.helper.ServiceIntegrationTestHelper;

class WorkspaceCreateServiceTest extends ServiceIntegrationTestHelper {

	@AfterEach
	public void tearDown() {
		databaseCleaner.execute();
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

	/*
	 * Todo
	 *  - 워크스페이스의 주인(생성자)의 로그인 Id를 통해서 주인 멤버 찾기
	 *  - 해당 멤버의 workspaceCount 감소
	 *  - 추후에 로직 개선 필요(DD를 적용하면 전체적으로 변할 듯)
	 *  - 주인(Owner) Role 추가 -> 그냥 워크스페이스 삭제를 주인만 가능하도록 제한하면 더 쉬울 듯
	 *  - -> 컨트롤러에서 LoginMember(id)를 읽어와서 사용하면 됨
	 *  <br>
	 *  - 해당 workspaceDelete의 로직을 변경하고 나서 테스트 실행
	 */
	@Disabled
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
		workspaceCommandService.deleteWorkspace(new WorkspaceDeleteRequest(), response.getCode());

		// then
		Member updatedMember = memberRepository.findById(member.getId()).get();
		assertThat(updatedMember.getWorkspaceCount()).isEqualTo(0);
	}
}
