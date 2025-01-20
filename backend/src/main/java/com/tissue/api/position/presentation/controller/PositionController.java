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
import com.tissue.api.position.presentation.dto.response.CreatePositionResponse;
import com.tissue.api.position.presentation.dto.response.DeletePositionResponse;
import com.tissue.api.position.presentation.dto.response.GetPositionsResponse;
import com.tissue.api.position.presentation.dto.response.UpdatePositionColorResponse;
import com.tissue.api.position.presentation.dto.response.UpdatePositionResponse;
import com.tissue.api.position.service.command.PositionCommandService;
import com.tissue.api.position.service.query.PositionQueryService;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

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
	public ApiResponse<CreatePositionResponse> createPosition(
		@PathVariable String code,
		@Valid @RequestBody CreatePositionRequest request
	) {
		CreatePositionResponse response = positionCommandService.createPosition(code, request);
		return ApiResponse.created("Position created.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@PatchMapping("/{positionId}")
	public ApiResponse<UpdatePositionResponse> updatePosition(
		@PathVariable String code,
		@PathVariable Long positionId,
		@Valid @RequestBody UpdatePositionRequest request
	) {
		UpdatePositionResponse response = positionCommandService.updatePosition(code, positionId, request);
		return ApiResponse.ok("Position updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@PatchMapping("/{positionId}/color")
	public ApiResponse<UpdatePositionColorResponse> updatePositionColor(
		@PathVariable String code,
		@PathVariable Long positionId,
		@Valid @RequestBody UpdatePositionColorRequest request
	) {
		UpdatePositionColorResponse response = positionCommandService.updatePositionColor(code, positionId, request);
		return ApiResponse.ok("Position color updated.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MANAGER)
	@DeleteMapping("/{positionId}")
	public ApiResponse<DeletePositionResponse> deletePosition(
		@PathVariable String code,
		@PathVariable Long positionId
	) {
		DeletePositionResponse response = positionCommandService.deletePosition(code, positionId);
		return ApiResponse.ok("Position deleted.", response);
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
