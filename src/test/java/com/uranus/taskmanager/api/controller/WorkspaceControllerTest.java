package com.uranus.taskmanager.api.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
	@DisplayName("POST /workspaces: 검증을 통과하면 CREATED를 기대한다")
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
	@DisplayName("POST /workspaces: (@NotBlank 검증) name과 description은 null, 빈 문자열, 공백이면 안된다.")
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

	@Test
	@DisplayName("POST /workspaces: (@Size 검증) name의 범위는 2~50자, description은 1~255자를 지켜야한다.")
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
	public void test4() throws Exception {

	}

	private String createLongString(int length) {
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append('a');
		}
		return sb.toString();
	}
}
