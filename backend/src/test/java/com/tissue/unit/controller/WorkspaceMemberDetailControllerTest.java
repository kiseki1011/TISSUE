package com.tissue.unit.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.position.presentation.dto.response.PositionDetail;
import com.tissue.api.workspacemember.presentation.dto.response.AssignPositionResponse;
import com.tissue.support.helper.ControllerTestHelper;

class WorkspaceMemberDetailControllerTest extends ControllerTestHelper {

	private static final String BASE_URL = "/api/v1/workspaces/{workspaceCode}/members";

	@Test
	@DisplayName("PATCH /workspaces/{workspaceCode}/members/{memberId}/positions/{positionId} - 내 Position 할당을 성공하면 OK 응답")
	void assignPosition_Success() throws Exception {
		// Given
		String workspaceCode = "TESTCODE";
		Long targetMemberId = 1L;
		Long loginMemberId = 1L;
		Long positionId = 1L;

		AssignPositionResponse response = new AssignPositionResponse(
			1L,
			List.of(PositionDetail.builder()
				.name("Developer")
				.description("Developer description")
				.color(ColorType.BLACK)
				.build()),
			LocalDateTime.now()
		);

		when(workspaceMemberCommandService.setPosition(
			workspaceCode,
			positionId,
			targetMemberId,
			loginMemberId
		))
			.thenReturn(response);

		// When & Then
		mockMvc.perform(
				patch(BASE_URL + "/{memberId}/positions/{positionId}", workspaceCode, targetMemberId, positionId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Position assigned to workspace member."))
			.andExpect(jsonPath("$.data.workspaceMemberId").value(1))
			.andExpect(jsonPath("$.data.assignedPositions[0].name").value("Developer"));
	}

	@Test
	@DisplayName("PATCH /workspaces/{workspaceCode}/members/positions/{positionId} - 내 Position 해제를 성공하면 OK 응답, 응답 데이터 없음")
	void removePosition_Success() throws Exception {
		// Given
		String workspaceCode = "TESTCODE";
		Long targetMemberId = 1L;
		Long loginMemberId = 2L;
		Long positionId = 1L;

		doNothing().when(workspaceMemberCommandService)
			.clearPosition(workspaceCode, positionId, targetMemberId, loginMemberId);

		// When & Then
		mockMvc.perform(
				delete(BASE_URL + "/{memberId}/positions/{positionId}", workspaceCode, targetMemberId, positionId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Position cleared from workspace member."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/members/{memberId}/positions/{positionId} - 다른 멤버의 Position 할당을 성공하면 OK 응답")
	void assignMemberPosition_Success() throws Exception {
		// Given
		String workspaceCode = "TESTCODE";
		Long positionId = 1L;

		Long targetMemberId = 1L;
		Long loginMemberId = 2L;

		AssignPositionResponse response = new AssignPositionResponse(
			targetMemberId,
			List.of(PositionDetail.builder()
				.name("Developer")
				.description("Developer description")
				.color(ColorType.BLACK)
				.build()),
			LocalDateTime.now()
		);

		when(workspaceMemberCommandService.setPosition(
			anyString(),
			anyLong(),
			anyLong(),
			anyLong()
		))
			.thenReturn(response);

		// When & Then
		mockMvc.perform(
				patch(BASE_URL + "/{memberId}/positions/{positionId}", workspaceCode, targetMemberId, positionId)
					.sessionAttr("id", loginMemberId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Position assigned to workspace member."))
			.andExpect(jsonPath("$.data.workspaceMemberId").value(targetMemberId))
			.andExpect(jsonPath("$.data.assignedPositions[0].name").value("Developer"));
	}
}