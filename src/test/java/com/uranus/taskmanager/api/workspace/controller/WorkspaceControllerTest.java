package com.uranus.taskmanager.api.workspace.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uranus.taskmanager.api.auth.SessionKey;
import com.uranus.taskmanager.api.auth.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.auth.service.AuthenticationService;
import com.uranus.taskmanager.api.global.config.WebMvcConfig;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.repository.InvitationRepository;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.member.service.MemberService;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceResponse;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.service.CheckCodeDuplicationService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceService;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.fixture.TestFixture;

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
	private MemberService memberService;
	@MockBean
	private AuthenticationService authenticationService;
	@MockBean
	private CheckCodeDuplicationService workspaceCreateService;
	@MockBean
	private MemberRepository memberRepository;
	@MockBean
	private WorkspaceRepository workspaceRepository;
	@MockBean
	private WorkspaceMemberRepository workspaceMemberRepository;
	@MockBean
	private InvitationRepository invitationRepository;
	@MockBean
	private WebMvcConfig webMvcConfig;

	TestFixture testFixture;

	@BeforeEach
	public void setup() {
		testFixture = new TestFixture();
	}

	@Test
	@DisplayName("워크스페이스 생성을 성공하면 CREATED를 응답한다")
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
	@DisplayName("워크스페이스 생성 시 이름과 설명은 null, 빈 문자열 또는 공백이면 안된다")
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
	@DisplayName("워크스페이스 생성 시 이름의 범위는 2~50자, 설명은 1~255자를 지켜야한다")
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
	@DisplayName("워크스페이스 조회를 성공하면 OK를 응답한다")
	public void test4() throws Exception {
		String code = "ABCD1234";
		WorkspaceResponse workspaceResponse = WorkspaceResponse.builder()
			.name("Test workspace")
			.description("Test description")
			.code(code)
			.build();
		when(workspaceService.get(code)).thenReturn(workspaceResponse);

		mockMvc.perform(get("/api/v1/workspaces/{code}", code))
			.andExpect(status().isOk())
			.andDo(print());
	}

	@Test
	@DisplayName("워크스페이스 코드로 조회가 가능하다")
	public void test5() throws Exception {
		String code = "ABCD1234";
		WorkspaceResponse workspaceResponse = WorkspaceResponse.builder()
			.name("Test workspace")
			.description("Test description")
			.code(code)
			.build();
		when(workspaceService.get(code)).thenReturn(workspaceResponse);

		mockMvc.perform(get("/api/v1/workspaces/{code}", code))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.code").value(code))
			.andExpect(jsonPath("$.data.name").value("Test workspace"))
			.andExpect(jsonPath("$.data.description").value("Test description"))
			.andDo(print());
	}

	@Test
	@DisplayName("워크스페이스 초대를 성공하면 초대 응답 객체를 데이터로 받는다")
	void test6() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";

		String invitedLoginId = "inviteduser123";

		Workspace workspace = testFixture.createWorkspace(workspaceCode);
		Member member = testFixture.createMember(loginId, email);

		Invitation invitation = testFixture.createPendingInvitation(workspace, member);

		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest(invitedLoginId);
		log.info("inviteMemberRequest = {}", inviteMemberRequest);
		String requestBody = objectMapper.writeValueAsString(inviteMemberRequest);

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER, loginId);

		InviteMemberResponse inviteMemberResponse = InviteMemberResponse.fromEntity(invitation);
		log.info("inviteMemberResponse = {}", inviteMemberResponse);

		// Todo: any()를 사용하지 않고 eq() 또는 객체 그대로 사용하는 경우 inviteMemberResponse가 null로 찍히는 문제 발생.
		//  정확한 객체에 대한 검증을 수행할 해결방법 찾아보기.
		when(workspaceService.inviteMember(eq(workspaceCode), ArgumentMatchers.any(InviteMemberRequest.class),
			ArgumentMatchers.any(LoginMemberDto.class)))
			.thenReturn(inviteMemberResponse);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/invite", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody)
				.session(session))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.code").value(workspaceCode))
			.andDo(print());
	}

	@Disabled
	@Test
	@DisplayName("해당 워크스페이스에서 ADMIN 권한이 있는 멤버는 초대 API 호출이 가능하다")
	void test7() throws Exception {
		// Todo
	}

	@Disabled
	@Test
	@DisplayName("해당 워크스페이스에서 ADMIN 권한이 없는 멤버가 초대를 시도하면 예외가 발생한다")
	void test8() throws Exception {
		// Todo
	}
}
