package com.tissue.unit.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.tissue.api.common.enums.ColorType;
import com.tissue.api.position.presentation.dto.request.CreatePositionRequest;
import com.tissue.api.position.presentation.dto.request.UpdatePositionColorRequest;
import com.tissue.api.position.presentation.dto.request.UpdatePositionRequest;
import com.tissue.api.position.presentation.dto.response.GetPositionsResponse;
import com.tissue.api.position.presentation.dto.response.PositionDetail;
import com.tissue.api.position.presentation.dto.response.PositionResponse;
import com.tissue.support.helper.ControllerTestHelper;

class PositionControllerTest extends ControllerTestHelper {

	private static final String WORKSPACE_CODE = "TESTCODE";

	private static final String BASE_URL = "/api/v1/workspaces/" + WORKSPACE_CODE + "/positions";

	@Test
	@DisplayName("POST /workspaces/{code}/positions - Position 생성 성공하면 CREATED 응답")
	void createPosition_Success() throws Exception {
		// Given
		Long positionId = 1L;
		CreatePositionRequest request = new CreatePositionRequest(
			"Developer",
			"Backend Developer"
		);

		PositionResponse expectedResponse = new PositionResponse(
			WORKSPACE_CODE,
			positionId
		);

		when(positionCommandService.createPosition(WORKSPACE_CODE, request))
			.thenReturn(expectedResponse);

		// When & Then
		mockMvc.perform(post(BASE_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.message").value("Position created."))
			.andExpect(jsonPath("$.data.positionId").value(1L))
			.andDo(print());
	}

	@Test
	@DisplayName("POST /workspaces/{code}/positions - Position 생성 시 유효성 검증 실패하면 BAD_REQUEST 응답")
	void createPosition_ValidationFail() throws Exception {
		// Given
		CreatePositionRequest request = new CreatePositionRequest(
			"",
			""
		);

		// When & Then
		mockMvc.perform(post(BASE_URL)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("One or more fields have failed validation."))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/positions/{positionId} - Position 수정 성공하면 OK 응답")
	void updatePosition_Success() throws Exception {
		// Given
		Long positionId = 1L;
		UpdatePositionRequest request = new UpdatePositionRequest(
			"Senior Developer",
			"Senior Backend Developer"
		);

		PositionResponse expectedResponse = new PositionResponse(
			WORKSPACE_CODE,
			positionId
		);

		when(positionCommandService.updatePosition(WORKSPACE_CODE, positionId, request))
			.thenReturn(expectedResponse);

		// When & Then
		mockMvc.perform(patch(BASE_URL + "/{positionId}", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.positionId").value(positionId))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/positions/{positionId/color - Position 색의 수정에 성공하면 OK 응답")
	void updatePositionColor_Success() throws Exception {
		// Given
		Long positionId = 1L;
		UpdatePositionColorRequest request = new UpdatePositionColorRequest(
			ColorType.GREEN
		);

		PositionResponse expectedResponse = new PositionResponse(
			WORKSPACE_CODE,
			positionId
		);

		when(positionCommandService.updatePositionColor(WORKSPACE_CODE, positionId, request))
			.thenReturn(expectedResponse);

		// When & Then
		mockMvc.perform(patch(BASE_URL + "/{positionId}/color", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.positionId").value(positionId))
			.andDo(print());
	}

	@Test
	@DisplayName("PATCH /workspaces/{code}/positions/{positionId}/color - Position의 색 수정 요청에 잘못된 값이 들어오면 유효성 검증에 실패한다")
	void updatePositionColor_Fail() throws Exception {
		// Given - 잘못된 colorType 값을 포함한 JSON 문자열을 직접 생성
		String invalidRequestJson = """
			{
			   "colorType": "RAINBOW"
			}
			""";

		// When & Then
		mockMvc.perform(patch(BASE_URL + "/{positionId}/color", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidRequestJson))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value("Invalid enum value provided."))
			.andExpect(jsonPath("$.data").isEmpty())
			.andDo(print());
	}

	@Test
	@DisplayName("GET /workspaces/{code}/positions - Position 목록 조회 성공하면 OK")
	void getPositions_Success() throws Exception {
		// Given
		List<PositionDetail> positions = List.of(
			new PositionDetail(1L, "Developer", "Backend Developer", ColorType.BLACK, LocalDateTime.now(),
				LocalDateTime.now()),
			new PositionDetail(2L, "Designer", "UI/UX Designer", ColorType.BLACK, LocalDateTime.now(),
				LocalDateTime.now())
		);
		GetPositionsResponse expectedResponse = new GetPositionsResponse(positions);

		when(positionQueryService.getPositions(WORKSPACE_CODE))
			.thenReturn(expectedResponse);

		// When & Then
		mockMvc.perform(get(BASE_URL)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.positions", hasSize(2)))
			.andExpect(jsonPath("$.data.positions[0].name").value("Developer"))
			.andDo(print());
	}
}