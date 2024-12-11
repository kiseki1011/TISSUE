package com.uranus.taskmanager.api.position.presentation.controller;

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

import com.uranus.taskmanager.api.common.dto.ApiResponse;
import com.uranus.taskmanager.api.position.presentation.dto.request.CreatePositionRequest;
import com.uranus.taskmanager.api.position.presentation.dto.request.UpdatePositionColorRequest;
import com.uranus.taskmanager.api.position.presentation.dto.request.UpdatePositionRequest;
import com.uranus.taskmanager.api.position.presentation.dto.response.CreatePositionResponse;
import com.uranus.taskmanager.api.position.presentation.dto.response.DeletePositionResponse;
import com.uranus.taskmanager.api.position.presentation.dto.response.GetPositionsResponse;
import com.uranus.taskmanager.api.position.presentation.dto.response.UpdatePositionColorResponse;
import com.uranus.taskmanager.api.position.presentation.dto.response.UpdatePositionResponse;
import com.uranus.taskmanager.api.position.service.command.PositionCommandService;
import com.uranus.taskmanager.api.position.service.query.PositionQueryService;
import com.uranus.taskmanager.api.security.authentication.interceptor.LoginRequired;
import com.uranus.taskmanager.api.security.authorization.interceptor.RoleRequired;
import com.uranus.taskmanager.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/workspaces/{code}/positions")
@RequiredArgsConstructor
public class PositionController {

	private final PositionCommandService positionCommandService;
	private final PositionQueryService positionQueryService;

	/**
	 * Todo
	 *  - updatePositionColor 필요
	 *  - 색상 필드에 대한 기능을 추가하면서 같이 추가
	 *  - 색상 필드 적용 시, createPosition에는 초기에는 랜덤한 색상으로 설정하도록 만들 필요 있음
	 */

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
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
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
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
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
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
	@RoleRequired(roles = {WorkspaceRole.MANAGER})
	@DeleteMapping("/{positionId}")
	public ApiResponse<DeletePositionResponse> deletePosition(
		@PathVariable String code,
		@PathVariable Long positionId
	) {
		DeletePositionResponse response = positionCommandService.deletePosition(code, positionId);
		return ApiResponse.ok("Position deleted.", response);
	}

	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.VIEWER})
	@GetMapping
	public ApiResponse<GetPositionsResponse> getPositions(
		@PathVariable String code
	) {
		GetPositionsResponse response = positionQueryService.getPositions(code);
		return ApiResponse.ok("Positions retrieved.", response);
	}
}
