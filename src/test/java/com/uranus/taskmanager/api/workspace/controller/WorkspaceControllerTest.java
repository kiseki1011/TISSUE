package com.uranus.taskmanager.api.workspace.controller;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.params.provider.Arguments.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.global.config.WebMvcConfig;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.repository.InvitationRepository;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.exception.MemberNotFoundException;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.member.service.MemberService;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceUpdateDetail;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.request.InviteMembersRequest;
import com.uranus.taskmanager.api.workspace.dto.request.KickWorkspaceMemberRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceContentUpdateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceDeleteRequest;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceParticipateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.FailedInvitedMember;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.InviteMembersResponse;
import com.uranus.taskmanager.api.workspace.dto.response.InvitedMember;
import com.uranus.taskmanager.api.workspace.dto.response.KickWorkspaceMemberResponse;
import com.uranus.taskmanager.api.workspace.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceContentUpdateResponse;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceParticipateResponse;
import com.uranus.taskmanager.api.workspace.exception.InvalidWorkspacePasswordException;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.service.CheckCodeDuplicationService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceAccessService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceCommandService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceQueryService;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.fixture.entity.InvitationEntityFixture;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceMemberEntityFixture;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@WebMvcTest({WorkspaceController.class, WorkspaceAccessController.class})
class WorkspaceControllerTest {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private WorkspaceAccessService workspaceAccessService;
	@MockBean
	private MemberService memberService;
	@MockBean
	private CheckCodeDuplicationService workspaceCreateService;
	@MockBean
	private WorkspaceQueryService workspaceQueryService;
	@MockBean
	private WorkspaceCommandService workspaceCommandService;
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
	@DisplayName("워크스페이스 생성을 성공하면 201을 응답한다")
	public void test1() throws Exception {

		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name("Test Workspace")
			.description("Test Description")
			.build();

		mockMvc.perform(post("/api/v1/workspaces")
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
	@DisplayName("워크스페이스 생성 시 이름과 설명은 null, 빈 문자열 또는 공백이면 검증 오류가 발생한다")
	public void test2(String name, String description) throws Exception {
		WorkspaceCreateRequest request = WorkspaceCreateRequest.builder()
			.name(name)
			.description(description)
			.build();

		mockMvc.perform(post("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
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
	@DisplayName("워크스페이스 생성 시 이름의 범위는 2~50자, 설명은 1~255자를 지키지 않으면 400을 응답한다")
	public void test3() throws Exception {
		// given
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
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.data[*].message").value(hasItem(nameValidMsg)))
			.andExpect(jsonPath("$.data[*].message").value(hasItem(descriptionValidMsg)))
			.andDo(print());
	}

	@Test
	@DisplayName("GET /workspaces/{code} - 워크스페이스 상세 정보 조회를 성공하면 200을 응답한다")
	public void test5() throws Exception {
		// given
		String code = "ABCD1234";

		WorkspaceDetail workspaceDetail = WorkspaceDetail.builder()
			.name("Test Workspace")
			.description("Test Description")
			.role(WorkspaceRole.ADMIN)
			.code(code)
			.build();

		when(workspaceQueryService.getWorkspaceDetail(eq(code), ArgumentMatchers.any(LoginMemberDto.class)))
			.thenReturn(workspaceDetail);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces/{code}", code))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.code").value(code))
			.andExpect(jsonPath("$.data.name").value("Test Workspace"))
			.andExpect(jsonPath("$.data.description").value("Test Description"))
			.andDo(print());
	}

	@Test
	@DisplayName("GET /workspaces - 현재 참여하고 있는 모든 워크스페이스의 조회에 성공하면 기대하는 JSON 응답을 받는다")
	void getMyWorkspaces_shouldReturnCompleteJsonResponse() throws Exception {
		// given
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

		when(workspaceQueryService.getMyWorkspaces(ArgumentMatchers.any(LoginMemberDto.class),
			ArgumentMatchers.any(Pageable.class)))
			.thenReturn(response);

		// 기대하는 JSON 응답 생성
		String expectedJson = objectMapper.writeValueAsString(
			ApiResponse.ok("Currently joined Workspaces Found", response)
		);

		// when & then - 요청 및 전체 JSON 비교 검증
		mockMvc.perform(get("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk())
			.andExpect(content().json(expectedJson));

		verify(workspaceQueryService, times(1))
			.getMyWorkspaces(ArgumentMatchers.any(LoginMemberDto.class), ArgumentMatchers.any(Pageable.class));
	}

	@Test
	@DisplayName("GET /workspaces - 현재 참여하고 있는 모든 워크스페이스의 조회에 성공하면 200을 응답받는다")
	void getCurrentlyJoinedWorkspaces_shouldReturn200IfSuccess() throws Exception {
		// given
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

		when(workspaceQueryService.getMyWorkspaces(ArgumentMatchers.any(LoginMemberDto.class),
			ArgumentMatchers.any(Pageable.class)))
			.thenReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/workspaces")
				.contentType(MediaType.APPLICATION_JSON)
				.param("page", "0")
				.param("size", "10"))
			.andExpect(status().isOk());

		verify(workspaceQueryService, times(1))
			.getMyWorkspaces(ArgumentMatchers.any(LoginMemberDto.class), ArgumentMatchers.any(Pageable.class));

	}

	@Test
	@DisplayName("PATCH /workspaces/{code} - 워크스페이스 정보 수정 요청에 성공하면 200을 응답받는다")
	void updateWorkspaceContent_shouldReturnUpdatedContent() throws Exception {
		// given
		WorkspaceContentUpdateRequest request = new WorkspaceContentUpdateRequest("New Title", "New Description");
		WorkspaceUpdateDetail original = WorkspaceUpdateDetail.from(Workspace.builder().build());
		WorkspaceUpdateDetail updateTo = WorkspaceUpdateDetail.from(Workspace.builder().build());
		WorkspaceContentUpdateResponse response = new WorkspaceContentUpdateResponse(original, updateTo);

		when(workspaceCommandService.updateWorkspaceContent(ArgumentMatchers.any(WorkspaceContentUpdateRequest.class),
			eq("TEST1111")))
			.thenReturn(response);

		// when & then
		mockMvc.perform(patch("/api/v1/workspaces/{code}", "TEST1111")
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
		WorkspaceDeleteRequest request = new WorkspaceDeleteRequest("password1234!");

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{code}", "TEST1111")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Workspace Deleted"))
			.andExpect(jsonPath("$.data").value("TEST1111"));

		verify(workspaceCommandService, times(1))
			.deleteWorkspace(ArgumentMatchers.any(WorkspaceDeleteRequest.class), eq("TEST1111"));
	}

	@Test
	@DisplayName("워크스페이스 초대를 성공하면 초대 응답 객체를 데이터로 받는다")
	void test6() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";

		String invitedLoginId = "inviteduser123";

		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		Member member = memberEntityFixture.createMember(loginId, email);

		Invitation invitation = invitationEntityFixture.createPendingInvitation(workspace, member);

		InviteMemberRequest inviteMemberRequest = new InviteMemberRequest(invitedLoginId);
		String requestBody = objectMapper.writeValueAsString(inviteMemberRequest);

		InviteMemberResponse inviteMemberResponse = InviteMemberResponse.from(invitation);

		when(workspaceAccessService.inviteMember(eq(workspaceCode), ArgumentMatchers.any(InviteMemberRequest.class)))
			.thenReturn(inviteMemberResponse);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}/invite", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.code").value(workspaceCode))
			.andDo(print());
	}

	@Test
	@DisplayName("다수 멤버의 초대를 요청하는 경우 - 모든 멤버 초대 성공하는 경우 200을 응답받는다")
	void test9() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String member1 = "member1";
		String member2 = "member2";
		List<String> memberIdentifiers = List.of(member1, member2);
		InviteMembersRequest inviteMembersRequest = new InviteMembersRequest(memberIdentifiers);

		List<InvitedMember> successfulResponses = List.of(
			InvitedMember.builder().loginId(member1).email("member1@test.com").build(),
			InvitedMember.builder().loginId(member2).email("member2@test.com").build()
		);

		List<FailedInvitedMember> failedResponses = List.of();

		InviteMembersResponse inviteMembersResponse = new InviteMembersResponse(successfulResponses, failedResponses);

		when(workspaceAccessService.inviteMembers(eq(workspaceCode), ArgumentMatchers.any(InviteMembersRequest.class)))
			.thenReturn(inviteMembersResponse);

		// then
		mockMvc.perform(post("/api/v1/workspaces/{code}/invites", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(inviteMembersRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.invitedMembers[0].loginId").value(member1))
			.andExpect(jsonPath("$.data.invitedMembers[1].loginId").value(member2))
			.andExpect(jsonPath("$.data.failedInvitedMembers").isEmpty())
			.andDo(print());
	}

	@Test
	@DisplayName("다수 멤버의 초대를 요청하는 경우 - 일부 멤버 초대를 실패해도 200을 응답받는다")
	void test10() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String member1 = "member1";
		String member3 = "member3";
		String invalidMember = "invalidMember";

		List<String> memberIdentifiers = List.of(member1, invalidMember, member3);
		InviteMembersRequest inviteMembersRequest = new InviteMembersRequest(memberIdentifiers);

		List<InvitedMember> successfulResponses = List.of(
			InvitedMember.builder().loginId(member1).email("member1@test.com").build(),
			InvitedMember.builder().loginId(member3).email("member3@test.com").build()
		);

		List<FailedInvitedMember> failedResponses = List.of(
			FailedInvitedMember.builder()
				.identifier(invalidMember)
				.error(new MemberNotFoundException().getMessage())
				.build()
		);

		InviteMembersResponse inviteMembersResponse = InviteMembersResponse.builder()
			.invitedMembers(successfulResponses)
			.failedInvitedMembers(failedResponses)
			.build();

		// when
		when(workspaceAccessService.inviteMembers(eq(workspaceCode), ArgumentMatchers.any(InviteMembersRequest.class)))
			.thenReturn(inviteMembersResponse);

		// then
		mockMvc.perform(post("/api/v1/workspaces/{code}/invites", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(inviteMembersRequest)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.invitedMembers[0].loginId").value(member1))
			.andExpect(jsonPath("$.data.invitedMembers[1].loginId").value(member3))
			.andExpect(jsonPath("$.data.failedInvitedMembers[0].identifier").value(invalidMember))
			.andExpect(jsonPath("$.data.failedInvitedMembers[0].error").value("Member was not found"))
			.andDo(print());
	}

	@Test
	@DisplayName("워크스페이스 참여 요청을 성공하는 경우 200을 응답 받는다")
	void test11() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String loginId = "user123";
		String email = "user123@test.com";
		String workspacePassword = "workspace1234!";

		Workspace workspace = workspaceEntityFixture.createWorkspaceWithPassword(workspaceCode, workspacePassword);
		Member member = memberEntityFixture.createMember(loginId, email);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createUserWorkspaceMember(member, workspace);
		WorkspaceParticipateRequest request = new WorkspaceParticipateRequest(workspace.getPassword());
		String requestBody = objectMapper.writeValueAsString(request);

		WorkspaceParticipateResponse response = WorkspaceParticipateResponse.from(workspace, workspaceMember, false);

		when(workspaceAccessService.joinWorkspace(eq(workspaceCode),
			ArgumentMatchers.any(WorkspaceParticipateRequest.class),
			ArgumentMatchers.any(LoginMemberDto.class))).thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Joined Workspace"))
			.andExpect(jsonPath("$.data.alreadyMember").value(false))
			.andDo(print());

	}

	@Test
	@DisplayName("워크스페이스 참여 요청 시 비밀번호가 불일치하는 경우 401을 응답 받는다")
	void test12() throws Exception {
		// given
		String workspaceCode = "TESTCODE";
		String invalidPassword = "invalid1234!";

		WorkspaceParticipateRequest request = new WorkspaceParticipateRequest(invalidPassword);

		when(workspaceAccessService.joinWorkspace(eq(workspaceCode),
			ArgumentMatchers.any(WorkspaceParticipateRequest.class),
			ArgumentMatchers.any(LoginMemberDto.class)))
			.thenThrow(new InvalidWorkspacePasswordException());

		// when & then
		mockMvc.perform(post("/api/v1/workspaces/{code}", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.message").value("The given workspace password is invalid"))
			.andDo(print());
	}

	@Test
	@DisplayName("워크스페이스에서 멤버를 추방하는데 성공하면 200을 응답받는다")
	void test13() throws Exception {
		// given
		String workspaceCode = "TESTCODE";

		Member member = memberEntityFixture.createMember("member1", "member1@test.com");
		Workspace workspace = workspaceEntityFixture.createWorkspace(workspaceCode);
		WorkspaceMember workspaceMember = workspaceMemberEntityFixture.createUserWorkspaceMember(member, workspace);

		KickWorkspaceMemberRequest request = new KickWorkspaceMemberRequest("member1");

		KickWorkspaceMemberResponse response = KickWorkspaceMemberResponse.from("member1", workspaceMember);

		when(workspaceAccessService.kickWorkspaceMember(workspaceCode, request)).thenReturn(response);

		// when & then
		mockMvc.perform(delete("/api/v1/workspaces/{code}/kick", workspaceCode)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Member was kicked from this Workspace"))
			.andDo(print());

	}
}
