package com.uranus.taskmanager.api.member.controller;

import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.InvalidMemberPasswordException;
import com.uranus.taskmanager.api.member.presentation.dto.request.MemberEmailUpdateRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.MemberWithdrawRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.SignupRequest;
import com.uranus.taskmanager.api.member.presentation.dto.request.UpdateAuthRequest;
import com.uranus.taskmanager.api.member.presentation.dto.response.MemberEmailUpdateResponse;
import com.uranus.taskmanager.api.member.presentation.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.security.authentication.session.SessionAttributes;
import com.uranus.taskmanager.api.workspace.presentation.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.helper.ControllerTestHelper;

class MemberControllerTest extends ControllerTestHelper {

	@Test
	@DisplayName("POST /members/signup - 회원 가입에 검증을 통과하면 CREATED를 기대한다")
	void test1() throws Exception {
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("testuser1234")
			.email("testemail@gmail.com")
			.password("Testpassword1234!")
			.build();
		String requestBody = objectMapper.writeValueAsString(signupRequest);

		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isCreated())
			.andDo(print());
	}

	@ParameterizedTest
	@CsvSource({
		"'testtesttesttesttest1', 'Login ID must be alphanumeric and must be between 2 and 20 characters'",
		"'1', 'Login ID must be alphanumeric and must be between 2 and 20 characters'",
		"'test!!', 'Login ID must be alphanumeric and must be between 2 and 20 characters'",
		"'한글아이디', 'Login ID must be alphanumeric and must be between 2 and 20 characters'",
		"'test1한글', 'Login ID must be alphanumeric and must be between 2 and 20 characters'",
	})
	@DisplayName("POST /members/signup - 회원 가입에 loginId는 영문과 숫자 조합에 2~20자를 지켜야한다")
	void test2(String loginId, String loginIdValidMsg) throws Exception {
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId(loginId)
			.email("testemail@gmail.com")
			.password("Testpassword1234!")
			.build();
		String requestBody = objectMapper.writeValueAsString(signupRequest);

		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data..message").value(loginIdValidMsg))
			.andDo(print());
	}

	@ParameterizedTest
	@CsvSource({
		"'test', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
		"'Test1234', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
		"'한글패스워드', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
		"'Test1234!한글', 'The password must be alphanumeric "
			+ "including at least one special character and must be between 8 and 30 characters'",
	})
	@DisplayName("POST /members/signup - 회원 가입에 password는 하나 이상의 영문자, 숫자와 특수문자를 포함한 조합에 8~30자를 지켜야한다")
	void test3(String password, String passwordValidMsg) throws Exception {
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId("testuser1234")
			.email("testemail@gmail.com")
			.password(password)
			.build();
		String requestBody = objectMapper.writeValueAsString(signupRequest);

		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data..message").value(passwordValidMsg))
			.andDo(print());
	}

	static Stream<Arguments> provideInvalidInputs() {
		return Stream.of(
			arguments(null, null, null), // null
			arguments("", "", ""), // 빈 문자열
			arguments(" ", " ", " ") // 공백
		);
	}

	@ParameterizedTest
	@MethodSource("provideInvalidInputs")
	@DisplayName("POST /members/signup - 회원 가입에 loginId, email, password는 null, 공백, 빈 문자이면 안된다")
	void test4(String loginId, String email, String password) throws Exception {
		// given
		SignupRequest signupRequest = SignupRequest.builder()
			.loginId(loginId)
			.email(email)
			.password(password)
			.build();

		// when & then
		mockMvc.perform(post("/api/v1/members/signup")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(signupRequest)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data[*].field").value(Matchers.hasItem("loginId")))
			.andExpect(jsonPath("$.data[*].field").value(Matchers.hasItem("email")))
			.andExpect(jsonPath("$.data[*].field").value(Matchers.hasItem("password")))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /members/update-auth - 업데이트 권한 요청에 성공하면 OK를 응답한다")
	void getUpdateAuthorization_success_OK() throws Exception {
		// given
		UpdateAuthRequest request = new UpdateAuthRequest("password1234!");

		doNothing()
			.when(memberQueryService)
			.validatePasswordForUpdate(any(UpdateAuthRequest.class), anyLong());

		// when & then
		mockMvc.perform(post("/api/v1/members/update-auth")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Update authorization granted"))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /members/update-auth - 패스워드 검증에 실패하면 업데이트 권한 요청에 대해 UNAUTHORIZED를 응답한다")
	void getUpdateAuthorization_returnsUnauthorized_whenInvalidMemberPasswordException() throws Exception {
		// given
		UpdateAuthRequest request = new UpdateAuthRequest("password1234!");

		doThrow(new InvalidMemberPasswordException())
			.when(memberQueryService)
			.validatePasswordForUpdate(any(UpdateAuthRequest.class), anyLong());

		// when & then
		mockMvc.perform(post("/api/v1/members/update-auth")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("The given password is invalid"))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members/email - 이메일 업데이트를 성공하면 OK를 응답한다")
	void updateEmail_success_OK() throws Exception {
		// given
		MemberEmailUpdateRequest request = new MemberEmailUpdateRequest("newemail@test.com");

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.UPDATE_AUTH, true);
		session.setAttribute(SessionAttributes.UPDATE_AUTH_EXPIRES_AT, LocalDateTime.now().plusMinutes(5));

		// when & then
		mockMvc.perform(patch("/api/v1/members/email")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Email update success"))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members/email - 이메일 업데이트를 성공하면 이메일 업데이트 응답을 데이터로 받는다")
	void updateEmail_success_returnsMemberEmailUpdateResponse() throws Exception {
		// given
		MemberEmailUpdateRequest request = new MemberEmailUpdateRequest("newemail@test.com");

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.UPDATE_AUTH, true);
		session.setAttribute(SessionAttributes.UPDATE_AUTH_EXPIRES_AT, LocalDateTime.now().plusMinutes(5));

		MemberEmailUpdateResponse response = MemberEmailUpdateResponse.from(
			Member.builder().email("newemail@test.com").build());

		when(memberService.updateEmail(any(MemberEmailUpdateRequest.class), anyLong())).thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/members/email")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Email update success"))
			.andExpect(jsonPath("$.data.memberDetail.email").value("newemail@test.com"))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /members/email - 이메일 업데이트 요청 시 업데이트 권한이 없으면 FORBIDDEN을 응답한다")
	void updateEmail_forbidden_ifUpdateAuthIsInvalid() throws Exception {
		// given
		MemberEmailUpdateRequest request = new MemberEmailUpdateRequest("newemail@test.com");

		// when & then
		mockMvc.perform(patch("/api/v1/members/email")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value(
				"You do not have authorization for update or the authorization has expired"))
			.andDo(print());
	}

	@Test
	@DisplayName("GET /members/workspaces - 현재 참여하고 있는 모든 워크스페이스의 조회에 성공하면 기대하는 응답을 받는다")
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
			.role(WorkspaceRole.COLLABORATOR)
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
			.role(WorkspaceRole.COLLABORATOR)
			.build();

		MyWorkspacesResponse response = MyWorkspacesResponse.builder()
			.workspaces(List.of(workspaceDetail1, workspaceDetail2))
			.totalElements(2L)
			.build();

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		when(memberQueryService.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class)))
			.thenReturn(response);

		// 기대하는 JSON 응답 생성
		String expectedJson = objectMapper.writeValueAsString(
			ApiResponse.ok("Currently joined Workspaces Found", response)
		);

		// when & then - 요청 및 전체 JSON 비교 검증
		mockMvc.perform(get("/api/v1/members/workspaces")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(content().json(expectedJson))
			.andDo(print());

		verify(memberQueryService, times(1))
			.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class));
	}

	@Test
	@DisplayName("GET /members/workspaces - 현재 참여하고 있는 모든 워크스페이스의 조회에 성공하면 OK를 응답받는다")
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
			.role(WorkspaceRole.COLLABORATOR)
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
			.role(WorkspaceRole.MANAGER)
			.build();

		MyWorkspacesResponse response = MyWorkspacesResponse.builder()
			.workspaces(List.of(workspaceDetail1, workspaceDetail2))
			.totalElements(2L)
			.build();

		when(memberQueryService.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/members/workspaces")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk());

		verify(memberQueryService, times(1))
			.getMyWorkspaces(anyLong(), ArgumentMatchers.any(Pageable.class));

	}

	@Test
	@DisplayName("DELETE /members - 멤버의 회원 탈퇴에 성공하면 OK를 응답받는다")
	void withdrawMember_shouldReturn200_ifSuccess() throws Exception {
		// Mock Session
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.UPDATE_AUTH, true);
		session.setAttribute(SessionAttributes.UPDATE_AUTH_EXPIRES_AT, LocalDateTime.now().plusMinutes(5));

		// given
		MemberWithdrawRequest request = new MemberWithdrawRequest("password1234!");

		// when & then
		mockMvc.perform(delete("/api/v1/members")
				.session(session)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member withdrawal success"))
			.andDo(print());
	}
}
