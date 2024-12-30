package com.tissue.api.workspace.presentation.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.tissue.api.security.session.SessionAttributes;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.presentation.dto.WorkspaceDetail;
import com.tissue.api.workspace.presentation.dto.request.CreateWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.request.DeleteWorkspaceRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateIssueKeyRequest;
import com.tissue.api.workspace.presentation.dto.request.UpdateWorkspaceInfoRequest;
import com.tissue.api.workspace.presentation.dto.response.DeleteWorkspaceResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateIssueKeyResponse;
import com.tissue.api.workspace.presentation.dto.response.UpdateWorkspaceInfoResponse;
import com.tissue.helper.ControllerTestHelper;

class WorkspaceControllerTest extends ControllerTestHelper {

	/**
	 * Todo
	 *  - 필드 검증에 대한 단위 테스트 작성법 찾아보기
	 *  - Q1: 같은 필드에 대해서 동일한 항목에 대해 검증 애노테이션이 겹치는 경우 어떻게 검증?
	 *    - 예시: @NotBlank와 @Size(min = 2, max = 50)을 적용한 필드에 " "(공백)가 들어가는 경우
	 *  - Q2: 검증 메세지 자체를 검증하는 것은 과연 효율적인가? 애노테이션 종류를 검증하는 것이 더 좋을지도?
	 */
	static Stream<Arguments> provideInvalidInputs() {
		return Stream.of(
			arguments(null, null), // null
			arguments("", ""),   // 빈 문자열
			arguments(" ", " ")  // 공백
		);
	}

	@Test
	@DisplayName("POST /workspaces - 워크스페이스 생성을 성공하면 CREATED를 응답한다")
	void test1() throws Exception {
		// given
		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andDo(print());
	}

	@ParameterizedTest
	@MethodSource("provideInvalidInputs")
	@DisplayName("POST /workspaces - 워크스페이스 생성 요청에서 이름과 설명은 null, 빈 문자열 또는 공백이면 검증 오류가 발생한다")
	void test2(String name, String description) throws Exception {
		// given
		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name(name)
			.description(description)
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/workspaces")
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
	@DisplayName("POST /workspaces - 워크스페이스 생성 요청에서 이름의 범위는 2~50자, 설명은 1~255자를 지키지 않으면 BAD_REQUEST 응답한다")
	void test3() throws Exception {
		// given
		String longName = createLongString(51);
		String longDescription = createLongString(256);
		String nameValidMsg = "Workspace name must be 2 ~ 50 characters long";
		String descriptionValidMsg = "Workspace description must be 1 ~ 255 characters long";

		CreateWorkspaceRequest request = CreateWorkspaceRequest.builder()
			.name(longName)
			.description(longDescription)
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data[*].message").value(hasItem(nameValidMsg)))
			.andExpect(jsonPath("$.data[*].message").value(hasItem(descriptionValidMsg)))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/info - 워크스페이스 정보 수정 요청에 성공하면 OK를 응답한다")
	void updateWorkspaceContent_shouldReturnUpdatedContent() throws Exception {
		// given
		UpdateWorkspaceInfoRequest request = new UpdateWorkspaceInfoRequest("New Title", "New Description");

		Workspace workspace = Workspace.builder()
			.code("TESTCODE")
			.name("New Title")
			.description("New Description")
			.build();

		UpdateWorkspaceInfoResponse response = UpdateWorkspaceInfoResponse.from(workspace);

		when(workspaceCommandService.updateWorkspaceInfo(
			ArgumentMatchers.any(UpdateWorkspaceInfoRequest.class),
			eq("TESTCODE")))
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/info", "TESTCODE")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Workspace info updated."))
			.andExpect(jsonPath("$.data.code").value("TESTCODE"))
			.andDo(print());

		verify(workspaceCommandService, times(1))
			.updateWorkspaceInfo(ArgumentMatchers.any(UpdateWorkspaceInfoRequest.class), eq("TESTCODE"));
	}

	@Test
	@DisplayName("DELETE /workspaces/{code} - 워크스페이스 삭제 요청에 성공하면 OK를 응답한다")
	void deleteWorkspace_shouldReturnSuccess() throws Exception {
		// given
		DeleteWorkspaceRequest request = new DeleteWorkspaceRequest("password1234!");

		Workspace workspace = Workspace.builder()
			.code("TESTCODE")
			.build();

		when(workspaceCommandService.deleteWorkspace(eq("TESTCODE"), anyLong()))
			.thenReturn(DeleteWorkspaceResponse.from(workspace));

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{code}", "TESTCODE")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Workspace deleted."))
			.andExpect(jsonPath("$.data.code").value("TESTCODE"));

		verify(workspaceCommandService, times(1))
			.deleteWorkspace(eq("TESTCODE"), anyLong());
	}

	@Test
	@DisplayName("GET /workspaces/{code} - 워크스페이스 상세 정보 조회를 성공하면 OK를 응답한다")
	void test5() throws Exception {
		// given
		String code = "ABCD1234";

		WorkspaceDetail workspaceDetail = WorkspaceDetail.builder()
			.name("Test Workspace")
			.description("Test Description")
			.code(code)
			.build();

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		when(workspaceQueryService.getWorkspaceDetail(code))
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
	@DisplayName("PATCH /workspaces/{code}/key - key prefix 수정에 성공하면 OK를 응답한다")
	void updateWorkspaceIssueKeyPrefix_shouldReturnOK() throws Exception {
		// given
		UpdateIssueKeyRequest request = new UpdateIssueKeyRequest("TESTPREFIX");

		UpdateIssueKeyResponse response = UpdateIssueKeyResponse.builder()
			.workspaceId(1L)
			.workspaceCode("TESTCODE")
			.keyPrefix("TESTPREFIX")
			.build();

		when(workspaceCommandService.updateIssueKey("TESTCODE", request))
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/key", "TESTCODE")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Issue key prefix updated."))
			.andExpect(jsonPath("$.data.workspaceCode").value("TESTCODE"))
			.andExpect(jsonPath("$.data.keyPrefix").value("TESTPREFIX"))
			.andDo(print());

		verify(workspaceCommandService, times(1))
			.updateIssueKey("TESTCODE", request);
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/key - key prefix 수정 요청에 영문자 외 사용하면 검증을 실패한다")
	void updateWorkspaceIssueKeyPrefix_failsRequestValidation() throws Exception {
		// given
		UpdateIssueKeyRequest request = new UpdateIssueKeyRequest("잘못된접두사");

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}/key", "TESTCODE")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}
}
