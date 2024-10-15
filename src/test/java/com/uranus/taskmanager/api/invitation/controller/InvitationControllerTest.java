package com.uranus.taskmanager.api.invitation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uranus.taskmanager.api.authentication.SessionKey;
import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.global.config.WebMvcConfig;
import com.uranus.taskmanager.api.invitation.domain.Invitation;
import com.uranus.taskmanager.api.invitation.dto.response.InvitationAcceptResponse;
import com.uranus.taskmanager.api.invitation.exception.InvitationNotFoundException;
import com.uranus.taskmanager.api.invitation.service.InvitationService;
import com.uranus.taskmanager.api.member.domain.Member;
import com.uranus.taskmanager.api.member.repository.MemberRepository;
import com.uranus.taskmanager.api.workspace.domain.Workspace;
import com.uranus.taskmanager.api.workspace.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.workspace.service.CheckCodeDuplicationService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceService;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceMember;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;
import com.uranus.taskmanager.fixture.TestFixture;

@WebMvcTest(controllers = InvitationController.class)
class InvitationControllerTest {

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
	private InvitationService invitationService;
	@MockBean
	private WebMvcConfig webMvcConfig;

	TestFixture testFixture;

	@BeforeEach
	public void setup() {
		testFixture = new TestFixture();
	}

	@Test
	@DisplayName("초대를 수락이 성공하면 OK를 응답받고 초대 수락 응답 DTO를 데이터로 받는다")
	void test1() throws Exception {
		// given
		String workspaceCode = "testcode";
		String loginId = "user123";
		String email = "user123@test.com";

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER, loginId);

		Workspace workspace = testFixture.createWorkspace(workspaceCode);
		Member member = testFixture.createMember(loginId, email);
		Invitation invitation = testFixture.createPendingInvitation(workspace, member);

		InvitationAcceptResponse acceptResponse = InvitationAcceptResponse.fromEntity(invitation,
			WorkspaceMember.addWorkspaceMember(member, workspace, WorkspaceRole.USER,
				member.getEmail()));

		when(invitationService.acceptInvitation(any(LoginMemberDto.class), eq(workspaceCode)))
			.thenReturn(acceptResponse);

		// when & then
		mockMvc.perform(post("/api/v1/invitations/{workspaceCode}/accept", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Invitation Accepted"))
			.andExpect(jsonPath("$.data").exists())
			.andDo(print());
	}

	@Test
	@DisplayName("유효하지 않은 코드로 초대를 수락하면 예외가 발생한다")
	void test2() throws Exception {
		// given
		String workspaceCode = "invalidcode";
		String loginId = "user123";

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER, loginId);

		when(invitationService.acceptInvitation(any(LoginMemberDto.class), eq(workspaceCode)))
			.thenThrow(new InvitationNotFoundException());

		// when & then
		mockMvc.perform(post("/api/v1/invitations/{workspaceCode}/accept", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON))
			// Todo: 추후 ResponseEntity를 적용하면 응답 상태 헤더는 해당 예외에서 상태를 꺼내서 사용한다 (400 -> 404로 변경)
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Invitation was not found for the given code"))
			.andDo(print());
	}

	@Test
	@DisplayName("초대를 거절하면 응답으로 OK를 받는다")
	void test3() throws Exception {
		// given
		String workspaceCode = "testcode";
		String loginId = "user123";

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionKey.LOGIN_MEMBER, loginId);

		// when & then
		mockMvc.perform(post("/api/v1/invitations/{workspaceCode}/reject", workspaceCode)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Invitation Rejected"))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andDo(print());
	}

}
