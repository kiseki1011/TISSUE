package com.uranus.taskmanager.api.workspacemember.presentation.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.uranus.taskmanager.api.workspacemember.presentation.dto.response.AssignPositionResponse;
import com.uranus.taskmanager.helper.ControllerTestHelper;

class WorkspaceMemberInfoControllerTest extends ControllerTestHelper {

	private static final String BASE_URL = "/api/v1/workspaces/{code}/members";

	@Test
	@DisplayName("PATCH /workspaces/{code}/members/positions/{positionId} - Position 할당을 성공하면 OK 응답")
	void assignPosition_Success() throws Exception {
		// Given
		String workspaceCode = "TESTCODE";
		Long loginMemberId = 1L;
		Long positionId = 1L;

		AssignPositionResponse response = new AssignPositionResponse(
			1L,
			"Developer",
			LocalDateTime.now()
		);

		when(workspaceMemberCommandService.assignPosition(
			workspaceCode,
			positionId,
			loginMemberId
		))
			.thenReturn(response);

		// When & Then
		mockMvc.perform(patch(BASE_URL + "/positions/{positionId}", workspaceCode, positionId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Position assigned to member."))
			.andExpect(jsonPath("$.data.workspaceMemberId").value(1))
			.andExpect(jsonPath("$.data.assignedPosition").value("Developer"));
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/members/positions - Position 해제를 성공하면 OK 응답, 응답 데이터 없음")
	void removePosition_Success() throws Exception {
		// Given
		String workspaceCode = "TESTCODE";
		Long loginMemberId = 1L;

		doNothing().when(workspaceMemberCommandService).removePosition(workspaceCode, loginMemberId);

		// When & Then
		mockMvc.perform(patch(BASE_URL + "/positions", workspaceCode))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.message").value("Position removed from member."))
			.andExpect(jsonPath("$.data").doesNotExist());
	}
}