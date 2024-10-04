package com.uranus.taskmanager.api.workspace.controller;

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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uranus.taskmanager.api.auth.SessionKey;
import com.uranus.taskmanager.api.global.config.WebMvcConfig;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceResponse;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.service.CheckCodeDuplicationService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceService;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;

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
	@MockBean
	private CheckCodeDuplicationService workspaceCreateService;
	@MockBean
	private MemberRepository memberRepository;
	@MockBean
	private WorkspaceRepository workspaceRepository;
	@MockBean
	private WorkspaceMemberRepository workspaceMemberRepository;
	@MockBean
	private WebMvcConfig webMvcConfig;

	@Test
	@DisplayName("워크스페이스 생성: 검증을 통과하면 CREATED를 기대한다")
	public void test1() throws Exception {

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER, "user123");

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();
		String requestBody = objectMapper.writeValueAsString(request);

		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody)
				.session(session))
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
	@DisplayName("워크스페이스 생성: name과 description은 null, 빈 문자열, 공백이면 안된다")
	public void test2(String name, String description) throws Exception {
		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name(name)
			.description(description)
			.build();
		String requestBody = objectMapper.writeValueAsString(request);

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER, "user123");

		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody)
				.session(session))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("One or more fields have validation errors"))
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
	@DisplayName("워크스페이스 생성: name의 범위는 2~50자, description은 1~255자를 지켜야한다")
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

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER, "user123");

		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody)
				.session(session))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data[*].message").value(hasItem(nameValidMsg)))
			.andExpect(jsonPath("$.data[*].message").value(hasItem(descriptionValidMsg)))
			.andDo(print());
	}

	@Test
	@DisplayName("워크스페이스 조회: 성공하면 OK를 기대한다")
	public void test4() throws Exception {
		String workspaceCode = UUID.randomUUID().toString();
		WorkspaceResponse workspaceResponse = WorkspaceResponse.builder()
			.name("Test workspace")
			.description("Test description")
			.workspaceCode(workspaceCode)
			.build();
		when(workspaceService.get(workspaceCode)).thenReturn(workspaceResponse);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceCode}", workspaceCode))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("워크스페이스 조회: workspaceCode로 할 수 있다")
	public void test5() throws Exception {
		String workspaceCode = UUID.randomUUID().toString();
		WorkspaceResponse workspaceResponse = WorkspaceResponse.builder()
			.name("Test workspace")
			.description("Test description")
			.workspaceCode(workspaceCode)
			.build();
		when(workspaceService.get(workspaceCode)).thenReturn(workspaceResponse);

		mockMvc.perform(get("/api/v1/workspaces/{workspaceCode}", workspaceCode))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.workspaceCode").value(workspaceCode))
			.andExpect(jsonPath("$.data.name").value("Test workspace"))
			.andExpect(jsonPath("$.data.description").value("Test description"));
	}

}
