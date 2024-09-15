package com.uranus.taskmanager.api.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uranus.taskmanager.api.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.response.WorkspaceResponse;
import com.uranus.taskmanager.api.service.WorkspaceService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest(WorkspaceController.class)
class WorkspaceControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private WorkspaceService workspaceService;

	@Test
	@DisplayName("워크스페이스 생성 요청 시 검증을 통과하면 CREATED를 기대한다")
	public void test1() throws Exception {

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();
		String requestBody = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andDo(print());
	}

	static Stream<Arguments> provideInvalidInputs() {
		String nameValidMsg = "Workspace name must not be blank";
		String descriptionValidMsg = "Workspace description must not be blank";
		return Stream.of(
			arguments(null, null, nameValidMsg, descriptionValidMsg), // null
			arguments("", "", nameValidMsg, descriptionValidMsg),   // 빈 문자열
			arguments(" ", " ", nameValidMsg, descriptionValidMsg)  // 공백
		);
	}

	@ParameterizedTest
	@MethodSource("provideInvalidInputs")
	@DisplayName("워크스페이스 생성 요청 시 name과 description은 null, 빈 문자열, 공백이면 안된다")
	public void test2(String name, String description, String nameValidMsg, String descriptionValidMsg) throws
		Exception {
		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name(name)
			.description(description)
			.build();
		String requestBody = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.validation.name").value(hasItem(nameValidMsg)))
			.andExpect(jsonPath("$.validation.description").value(hasItem(descriptionValidMsg)))
			.andDo(print());
	}

	private String createLongString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append('a');
		}
		return sb.toString();
	}

	@Test
	@DisplayName("워크스페이스 생성 요청 시 name의 범위는 2~50자, description은 1~255자를 지켜야한다")
	public void test3() throws Exception {
		String longName = createLongString(51);
		String longDescription = createLongString(256);
		String nameValidMsg = "Workspace name must be 2 ~ 50 characters long";
		String descriptionValidMsg = "Workspace name must be 1 ~ 255 characters long";

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name(longName)
			.description(longDescription)
			.build();
		String requestBody = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.validation.name").value(hasItem(nameValidMsg)))
			.andExpect(jsonPath("$.validation.description").value(hasItem(descriptionValidMsg)))
			.andDo(print());
	}

	@Test
	@DisplayName("워크스페이스 조회에 성공하면 OK를 기대한다")
	public void test4() throws Exception {
		String workspaceCode = UUID.randomUUID().toString();
		WorkspaceResponse workspaceResponse = WorkspaceResponse.builder()
			.id(1L)
			.name("Test workspace")
			.description("Test description")
			.workspaceCode(workspaceCode)
			.build();
		when(workspaceService.get(workspaceCode)).thenReturn(workspaceResponse);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceCode}", workspaceCode))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("워크스페이스 조회는 workspaceCode로 할 수 있다")
	public void test5() throws Exception {
		String workspaceCode = UUID.randomUUID().toString();
		WorkspaceResponse workspaceResponse = WorkspaceResponse.builder()
			.id(1L)
			.name("Test workspace")
			.description("Test description")
			.workspaceCode(workspaceCode)
			.build();
		when(workspaceService.get(workspaceCode)).thenReturn(workspaceResponse);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceCode}", workspaceCode))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(1L))
			.andExpect(jsonPath("$.workspaceCode").value(workspaceCode))
			.andExpect(jsonPath("$.name").value("Test workspace"))
			.andExpect(jsonPath("$.description").value("Test description"));
	}

}
