package com.uranus.taskmanager.api.workspacemember.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.security.session.SessionAttributes;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.request.JoinWorkspaceRequest;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.JoinWorkspaceResponse;
import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.fixture.entity.InvitationEntityFixture;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceMemberEntityFixture;
import com.uranus.taskmanager.helper.ControllerTestHelper;

class WorkspaceParticipationControllerTest extends ControllerTestHelper {

	WorkspaceEntityFixture workspaceEntityFixture;
	MemberEntityFixture memberEntityFixture;
	WorkspaceMemberEntityFixture workspaceMemberEntityFixture;
	InvitationEntityFixture invitationEntityFixture;

	@BeforeEach
	public void setup() {
		workspaceEntityFixture = new WorkspaceEntityFixture();
		memberEntityFixture = new MemberEntityFixture();
		workspaceMemberEntityFixture = new WorkspaceMemberEntityFixture();
		invitationEntityFixture = new InvitationEntityFixture();
	}

	@Test
	@DisplayName("POST /members/workspaces/{code} - 워크스페이스 참여 요청을 성공하는 경우 200을 응답 받는다")
	void test11() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		// given
		String workspaceCode = "TESTCODE";
		String loginId = "member1";
		String email = "member1@test.com";

		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		Member member = memberEntityFixture.createMember(loginId, email);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createCollaboratorWorkspaceMember(member,
			workspace);
		JoinWorkspaceRequest request = new JoinWorkspaceRequest();

		JoinWorkspaceResponse response = JoinWorkspaceResponse.from(workspaceMember);

		when(workspaceParticipationCommandService.joinWorkspace(eq(workspaceCode),
			any(JoinWorkspaceRequest.class),
			anyLong())).thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Joined workspace"))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /members/workspaces/{code} - 워크스페이스 참여 요청 시 비밀번호가 불일치하는 경우 401을 응답 받는다")
	void test12() throws Exception {
		// Session 모킹
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		// given
		String workspaceCode = "TESTCODE";
		String invalidPassword = "invalid1234!";

		JoinWorkspaceRequest request = new JoinWorkspaceRequest(invalidPassword);

		when(workspaceParticipationCommandService.joinWorkspace(eq("TESTCODE"), any(JoinWorkspaceRequest.class),
			anyLong()))
			.thenThrow(new InvalidWorkspacePasswordException());

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("The given workspace password is invalid"))
			.andDo(print());
	}

	@Test
	@DisplayName("GET /workspaces - 현재 참여하고 있는 모든 워크스페이스의 조회에 성공하면 기대하는 응답을 받는다")
	void getMyWorkspaces_shouldReturn_completeJsonResponse() throws Exception {
		// given
		WorkspaceDetail workspaceDetail1 = WorkspaceDetail.builder()
			.id(1L)
			.code("WS001")
			.name("Workspace 1")
			.description("Description 1")
			.createdBy("member1")
			.createdAt(LocalDateTime.now().minusDays(5))
			.updatedBy("updater1")
			.updatedAt(LocalDateTime.now())
			.build();

		WorkspaceDetail workspaceDetail2 = WorkspaceDetail.builder()
			.id(2L)
			.code("WS002")
			.name("Workspace 2")
			.description("Description 2")
			.createdBy("member1")
			.createdAt(LocalDateTime.now().minusDays(10))
			.updatedBy("updater2")
			.updatedAt(LocalDateTime.now())
			.build();

		MyWorkspacesResponse response = MyWorkspacesResponse.builder()
			.workspaces(List.of(workspaceDetail1, workspaceDetail2))
			.totalElements(2L)
			.build();

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		when(workspaceParticipationQueryService.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class)))
			.thenReturn(response);

		// 기대하는 JSON 응답 생성
		String expectedJson = objectMapper.writeValueAsString(
			ApiResponse.ok("Currently joined workspaces found.", response)
		);

		// when & then - 요청 및 전체 JSON 비교 검증
		mockMvc.perform(get("/api/v1/workspaces")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(content().json(expectedJson))
			.andDo(print());

		verify(workspaceParticipationQueryService, times(1))
			.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class));
	}

	@Test
	@DisplayName("GET /workspaces - 현재 참여하고 있는 모든 워크스페이스의 조회에 성공하면 OK를 응답받는다")
	void getCurrentlyJoinedWorkspaces_shouldReturn200_ifSuccess() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		WorkspaceDetail workspaceDetail1 = WorkspaceDetail.builder()
			.id(1L)
			.code("WS001")
			.name("Workspace 1")
			.description("Description 1")
			.createdBy("creator1")
			.createdAt(LocalDateTime.now().minusDays(5))
			.updatedBy("updater1")
			.updatedAt(LocalDateTime.now())
			.build();

		WorkspaceDetail workspaceDetail2 = WorkspaceDetail.builder()
			.id(2L)
			.code("WS002")
			.name("Workspace 2")
			.description("Description 2")
			.createdBy("creator2")
			.createdAt(LocalDateTime.now().minusDays(10))
			.updatedBy("updater2")
			.updatedAt(LocalDateTime.now())
			.build();

		MyWorkspacesResponse response = MyWorkspacesResponse.builder()
			.workspaces(List.of(workspaceDetail1, workspaceDetail2))
			.totalElements(2L)
			.build();

		when(workspaceParticipationQueryService.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk());

		verify(workspaceParticipationQueryService, times(1))
			.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class));

	}

}
