package com.uranus.taskmanager.api.invitation.presentation.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.uranus.taskmanager.api.invitation.exception.InvitationNotFoundException;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.AcceptInvitationResponse;
import com.uranus.taskmanager.api.security.session.SessionAttributes;
import com.uranus.taskmanager.fixture.entity.InvitationEntityFixture;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.helper.ControllerTestHelper;

class InvitationControllerTest extends ControllerTestHelper {

	WorkspaceEntityFixture workspaceEntityFixture;
	MemberEntityFixture memberEntityFixture;
	InvitationEntityFixture invitationEntityFixture;

	@BeforeEach
	public void setup() {
		workspaceEntityFixture = new WorkspaceEntityFixture();
		memberEntityFixture = new MemberEntityFixture();
		invitationEntityFixture = new InvitationEntityFixture();
	}

	@Test
	@DisplayName("POST /invitations/{invitationId}/accept - 초대를 수락이 성공하면 OK를 응답받는다")
	void test1() throws Exception {
		// given
		Long invitationId = 1L;

		AcceptInvitationResponse response = new AcceptInvitationResponse(invitationId, null, null);
		
		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, "1L");

		when(invitationService.acceptInvitation(anyLong(), eq(invitationId))).thenReturn(response);

		// when & then
		mockMvc.perform(post("/api/v1/invitations/{invitationId}/accept", invitationId)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Invitation Accepted."))
			.andExpect(jsonPath("$.data").exists())
			.andDo(print());
	}

	@Test
	@DisplayName("POST /invitations/{invitationId}/accept - 유효하지 않은 코드로 초대를 수락하면 예외가 발생한다")
	void test2() throws Exception {
		// given
		Long invalidInvitationId = 999L;

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, 1L);

		when(invitationService.acceptInvitation(1L, invalidInvitationId))
			.thenThrow(new InvitationNotFoundException());

		// when & then
		mockMvc.perform(post("/api/v1/invitations/{invalidInvitationId}/accept", invalidInvitationId)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.message").value("Invitation was not found for the given code"))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /invitations/{invitationId}/reject - 초대를 거절하면 응답으로 OK를 받는다")
	void test3() throws Exception {
		// given
		Long invitationId = 1L;

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, "1L");

		// when & then
		mockMvc.perform(post("/api/v1/invitations/{invitationId}/reject", invitationId)
				.session(session)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Invitation Rejected."))
			.andExpect(jsonPath("$.data").doesNotExist())
			.andDo(print());
	}

}
