package com.tissue.api.position.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.position.presentation.dto.request.CreatePositionRequest;
import com.tissue.api.position.presentation.dto.request.UpdatePositionColorRequest;
import com.tissue.api.position.presentation.dto.request.UpdatePositionRequest;
import com.tissue.api.position.presentation.dto.response.GetPositionsResponse;
import com.tissue.api.position.presentation.dto.response.PositionResponse;
import com.tissue.api.position.application.service.command.PositionCommandService;
import com.tissue.api.position.application.service.query.PositionQueryService;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{code}/positions")
@RequiredArgsConstructor
public class PositionController {

	private final PositionCommandService positionCommandService;
	private final PositionQueryService positionQueryService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<PositionResponse> createPosition(
		@PathVariable String code,
		@Valid @RequestBody CreatePositionRequest request
	) {
		PositionResponse response = positionCommandService.createPosition(code, request);
		return ApiResponse.created("Position created.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@PatchMapping("/{positionId}")
	public ApiResponse<PositionResponse> updatePosition(
		@PathVariable String code,
		@PathVariable Long positionId,
		@Valid @RequestBody UpdatePositionRequest request
	) {
		PositionResponse response = positionCommandService.updatePosition(code, positionId, request);
		return ApiResponse.ok("Position updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@PatchMapping("/{positionId}/color")
	public ApiResponse<PositionResponse> updatePositionColor(
		@PathVariable String code,
		@PathVariable Long positionId,
		@Valid @RequestBody UpdatePositionColorRequest request
	) {
		PositionResponse response = positionCommandService.updatePositionColor(code, positionId, request);
		return ApiResponse.ok("Position color updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping("/{positionId}")
	public ApiResponse<Void> deletePosition(
		@PathVariable String code,
		@PathVariable Long positionId
	) {
		positionCommandService.deletePosition(code, positionId);
		return ApiResponse.okWithNoContent("Position deleted.");
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping
	public ApiResponse<GetPositionsResponse> getPositions(
		@PathVariable String code
	) {
		GetPositionsResponse response = positionQueryService.getPositions(code);
		return ApiResponse.ok("Positions retrieved.", response);
	}
}
