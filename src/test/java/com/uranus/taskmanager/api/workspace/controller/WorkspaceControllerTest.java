package com.uranus.taskmanager.api.workspace.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.uranus.taskmanager.api.authentication.SessionKey;
import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceUpdateDetail;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceContentUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceDeleteRequest;
import com.uranus.taskmanager.api.workspace.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceContentUpdateResponse;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.helper.ControllerTestHelper;

class WorkspaceControllerTest extends ControllerTestHelper {

	@Test
	@DisplayName("POST /workspaces - 워크스페이스 생성을 성공하면 201을 응답한다")
	void test1() throws Exception {

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		mockMvc.perform(post("/api/v1/workspaces")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andDo(print());
	}

	/**
	 * Todo
	 * 필드 검증에 대한 단위 테스트 작성법 찾아보기
	 * Q1: 같은 필드에 대해서 동일한 항목에 대해 검증 애노테이션이 겹치는 경우 어떻게 검증?
	 * - 예시: @NotBlank와 @Size(min = 2, max = 50)을 적용한 필드에 " "(공백)가 들어가는 경우
	 * Q2: 검증 메세지 자체를 검증하는 것은 과연 효율적인가? 애노테이션 종류를 검증하는 것이 더 좋을지도?
	 */
	static Stream<Arguments> provideInvalidInputs() {
		return Stream.of(
			arguments(null, null), // null
			arguments("", ""),   // 빈 문자열
			arguments(" ", " ")  // 공백
		);
	}

	@ParameterizedTest
	@MethodSource("provideInvalidInputs")
	@DisplayName("POST /workspaces - 워크스페이스 생성 요청에서 이름과 설명은 null, 빈 문자열 또는 공백이면 검증 오류가 발생한다")
	void test2(String name, String description) throws Exception {

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name(name)
			.description(description)
			.build();

		mockMvc.perform(post("/api/v1/workspaces")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("One or more fields have validation errors"))
			.andDo(print());
	}

	private String createLongString(int length) {
		return "a".repeat(Math.max(0, length));
	}

	@Test
	@DisplayName("POST /workspaces - 워크스페이스 생성 요청에서 이름의 범위는 2~50자, 설명은 1~255자를 지키지 않으면 400을 응답한다")
	void test3() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		String longName = createLongString(51);
		String longDescription = createLongString(256);
		String nameValidMsg = "Workspace name must be 2 ~ 50 characters long";
		String descriptionValidMsg = "Workspace description must be 1 ~ 255 characters long";

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name(longName)
			.description(longDescription)
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/workspaces")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data[*].message").value(hasItem(nameValidMsg)))
			.andExpect(jsonPath("$.data[*].message").value(hasItem(descriptionValidMsg)))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code} - 워크스페이스 정보 수정 요청에 성공하면 200을 응답받는다")
	void updateWorkspaceContent_shouldReturnUpdatedContent() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		WorkspaceContentUpdateRequest request = new WorkspaceContentUpdateRequest("New Title", "New Description");
		WorkspaceUpdateDetail original = WorkspaceUpdateDetail.from(Workspace.builder().build());
		WorkspaceUpdateDetail updateTo = WorkspaceUpdateDetail.from(Workspace.builder().build());
		WorkspaceContentUpdateResponse response = new WorkspaceContentUpdateResponse(original, updateTo);

		// Workspace, WorkspaceMember 모의 객체 만들기
		Workspace workspace = Workspace.builder()
			.code("TEST1111")
			.build();
		WorkspaceMember workspaceMember = WorkspaceMember.builder()
			.role(WorkspaceRole.ADMIN)
			.build();

		// AuthorizationInterceptor 행위 모킹
		when(workspaceRepository.findByCode("TEST1111")).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findByMemberIdAndWorkspaceId(1L, null))
			.thenReturn(Optional.of(workspaceMember));

		when(workspaceCommandService.updateWorkspaceContent(ArgumentMatchers.any(WorkspaceContentUpdateRequest.class),
			eq("TEST1111")))
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}", "TEST1111")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Workspace Title and Description Updated"))
			.andDo(print());

		verify(workspaceCommandService, times(1))
			.updateWorkspaceContent(ArgumentMatchers.any(WorkspaceContentUpdateRequest.class), eq("TEST1111"));
	}

	@Test
	@DisplayName("DELETE /workspaces/{code} - 워크스페이스 삭제 요청에 성공하면 200을 응답받는다")
	void deleteWorkspace_shouldReturnSuccess() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		WorkspaceDeleteRequest request = new WorkspaceDeleteRequest("password1234!");

		// Workspace, WorkspaceMember 모의 객체 만들기
		Workspace workspace = Workspace.builder()
			.code("TEST1111")
			.build();
		WorkspaceMember workspaceMember = WorkspaceMember.builder()
			.role(WorkspaceRole.ADMIN)
			.build();

		// AuthorizationInterceptor 행위 모킹
		when(workspaceRepository.findByCode("TEST1111")).thenReturn(Optional.of(workspace));
		when(workspaceMemberRepository.findByMemberIdAndWorkspaceId(1L, null))
			.thenReturn(Optional.of(workspaceMember));

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{code}", "TEST1111")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Workspace Deleted"))
			.andExpect(jsonPath("$.data").value("TEST1111"));

		verify(workspaceCommandService, times(1))
			.deleteWorkspace(ArgumentMatchers.any(WorkspaceDeleteRequest.class), eq("TEST1111"));
	}

	@Test
	@DisplayName("GET /workspaces/{code} - 워크스페이스 상세 정보 조회를 성공하면 200을 응답한다")
	void test5() throws Exception {
		// given
		String code = "ABCD1234";

		WorkspaceDetail workspaceDetail = WorkspaceDetail.builder()
			.name("Test Workspace")
			.description("Test Description")
			.role(WorkspaceRole.ADMIN)
			.code(code)
			.build();

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		when(workspaceQueryService.getWorkspaceDetail(eq(code), anyLong()))
			.thenReturn(workspaceDetail);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{code}", code)
				.session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.code").value(code))
			.andExpect(jsonPath("$.data.name").value("Test Workspace"))
			.andExpect(jsonPath("$.data.description").value("Test Description"))
			.andDo(print());

	}

	@Test
	@DisplayName("GET /workspaces - 현재 참여하고 있는 모든 워크스페이스의 조회에 성공하면 기대하는 응답을 받는다")
	void getMyWorkspaces_shouldReturnCompleteJsonResponse() throws Exception {
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
			.role(WorkspaceRole.USER)
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
			.role(WorkspaceRole.USER)
			.build();

		MyWorkspacesResponse response = MyWorkspacesResponse.builder()
			.workspaces(List.of(workspaceDetail1, workspaceDetail2))
			.totalElements(2L)
			.build();

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		when(workspaceQueryService.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class)))
			.thenReturn(response);

		// 기대하는 JSON 응답 생성
		String expectedJson = objectMapper.writeValueAsString(
			ApiResponse.ok("Currently joined Workspaces Found", response)
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

		verify(workspaceQueryService, times(1))
			.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class));
	}

	@Test
	@DisplayName("GET /workspaces - 현재 참여하고 있는 모든 워크스페이스의 조회에 성공하면 200을 응답받는다")
	void getCurrentlyJoinedWorkspaces_shouldReturn200IfSuccess() throws Exception {
		// given
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER_ID, 1L);

		WorkspaceDetail workspaceDetail1 = WorkspaceDetail.builder()
			.id(1L)
			.code("WS001")
			.name("Workspace 1")
			.description("Description 1")
			.createdBy("creator1")
			.createdAt(LocalDateTime.now().minusDays(5))
			.updatedBy("updater1")
			.updatedAt(LocalDateTime.now())
			.role(WorkspaceRole.USER)
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
			.role(WorkspaceRole.ADMIN)
			.build();

		MyWorkspacesResponse response = MyWorkspacesResponse.builder()
			.workspaces(List.of(workspaceDetail1, workspaceDetail2))
			.totalElements(2L)
			.build();

		when(workspaceQueryService.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk());

		verify(workspaceQueryService, times(1))
			.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class));

	}

}
