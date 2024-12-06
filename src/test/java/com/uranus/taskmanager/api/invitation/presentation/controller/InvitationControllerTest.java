package com.uranus.taskmanager.api.invitation.presentation.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import com.uranus.taskmanager.api.invitation.domain.InvitationStatus;
import com.uranus.taskmanager.api.invitation.exception.InvitationNotFoundException;
import com.uranus.taskmanager.api.invitation.presentation.dto.InvitationSearchCondition;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.AcceptInvitationResponse;
import com.uranus.taskmanager.api.invitation.presentation.dto.response.InvitationResponse;
import com.uranus.taskmanager.api.security.session.SessionAttributes;
import com.uranus.taskmanager.fixture.entity.MemberEntityFixture;
import com.uranus.taskmanager.fixture.entity.WorkspaceEntityFixture;
import com.uranus.taskmanager.helper.ControllerTestHelper;

class InvitationControllerTest extends ControllerTestHelper {

	WorkspaceEntityFixture workspaceEntityFixture;
	MemberEntityFixture memberEntityFixture;

	@BeforeEach
	public void setup() {
		workspaceEntityFixture = new WorkspaceEntityFixture();
		memberEntityFixture = new MemberEntityFixture();
	}

	@Test
	@DisplayName("POST /invitations/{invitationId}/accept - 초대를 수락이 성공하면 OK를 응답받는다")
	void test1() throws Exception {
		// given
		Long invitationId = 1L;

		AcceptInvitationResponse response = new AcceptInvitationResponse(invitationId, null, null);

		MockHttpSession session = new MockHttpSession();
		session.setAttribute(SessionAttributes.LOGIN_MEMBER_ID, "1L");

		when(invitationCommandService.acceptInvitation(anyLong(), eq(invitationId))).thenReturn(response);

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

		when(invitationCommandService.acceptInvitation(1L, invalidInvitationId))
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

	@Test
	@DisplayName("GET /invitations - 자신의 초대 목록 조회 시 기본적으로 PENDING 상태의 초대들을 조회된다")
	void getMyInvitations_defaultStatusConditionIsPending() throws Exception {
		// given
		Long loginMemberId = 1L;
		InvitationSearchCondition searchCondition = new InvitationSearchCondition(List.of(InvitationStatus.PENDING));
		Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdDate"));

		List<InvitationResponse> content = List.of(
			new InvitationResponse(
				1L, "TESTCODE1",
				"inviter@test.com",
				InvitationStatus.PENDING,
				LocalDateTime.now()
			),
			new InvitationResponse(
				2L,
				"TESTCODE2",
				"inviter@test.com",
				InvitationStatus.PENDING,
				LocalDateTime.now()
			)
		);
		Page<InvitationResponse> page = new PageImpl<>(content, pageable, content.size());

		when(invitationQueryService.getInvitations(loginMemberId, searchCondition, pageable))
			.thenReturn(page);

		// when & then
		mockMvc.perform(get("/api/v1/invitations")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Found invitations"))
			.andExpect(jsonPath("$.data.content", hasSize(2)))
			.andExpect(jsonPath("$.data.content[0].invitationId").value(1))
			.andExpect(jsonPath("$.data.content[0].workspaceCode").value("TESTCODE1"))
			.andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
			.andExpect(jsonPath("$.data.pageInfo.totalElements").value(2))
			.andDo(print());
	}

	@Test
	@DisplayName("GET /invitations - 초대 목록 조회 시 상태를 필터링할 수 있다")
	void getMyInvitationsWithStatusFilter() throws Exception {
		// given
		Pageable pageable = PageRequest.of(
			0,
			20,
			Sort.by(Sort.Direction.DESC, "createdDate")
		);

		List<InvitationResponse> content = List.of(
			new InvitationResponse(
				1L,
				"TESTCODE1",
				"inviter@test.com",
				InvitationStatus.ACCEPTED,
				LocalDateTime.now()
			),
			new InvitationResponse(
				2L,
				"TESTCODE2",
				"inviter@test.com",
				InvitationStatus.REJECTED,
				LocalDateTime.now()
			)
		);

		Page<InvitationResponse> page = new PageImpl<>(content, pageable, content.size());

		when(invitationQueryService.getInvitations(
			anyLong(),
			ArgumentMatchers.any(InvitationSearchCondition.class),
			eq(pageable))
		)
			.thenReturn(page);

		// when & then
		mockMvc.perform(get("/api/v1/invitations")
				.param("statuses", "ACCEPTED", "REJECTED")
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Found invitations"))
			.andExpect(jsonPath("$.data.content[0].status").value("ACCEPTED"))
			.andExpect(jsonPath("$.data.content[1].status").value("REJECTED"))
			.andDo(print());
	}
}
