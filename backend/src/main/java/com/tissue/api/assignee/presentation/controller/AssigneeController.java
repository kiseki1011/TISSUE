package com.tissue.api.assignee.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.assignee.presentation.dto.request.AddAssigneeRequest;
import com.tissue.api.assignee.presentation.dto.request.RemoveAssigneeRequest;
import com.tissue.api.assignee.presentation.dto.response.AddAssigneeResponse;
import com.tissue.api.assignee.presentation.dto.response.RemoveAssigneeResponse;
import com.tissue.api.assignee.service.command.AssigneeCommandService;
import com.tissue.api.assignee.service.dto.AddAssigneeCommand;
import com.tissue.api.assignee.service.dto.RemoveAssigneeCommand;
import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{code}/issues/{issueKey}/assignees")
public class AssigneeController {

	private final AssigneeCommandService assigneeCommandService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ApiResponse<AddAssigneeResponse> addAssignee(
		@PathVariable String code,
		@PathVariable String issueKey,
		@RequestBody @Valid AddAssigneeRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		AddAssigneeCommand command = request.toCommand();

		AddAssigneeResponse response = assigneeCommandService.addAssignee(
			code,
			issueKey,
			command,
			loginMemberId
		);

		return ApiResponse.ok("Assignee added.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping
	public ApiResponse<RemoveAssigneeResponse> removeAssignee(
		@PathVariable String code,
		@PathVariable String issueKey,
		@RequestBody @Valid RemoveAssigneeRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		RemoveAssigneeCommand command = request.toCommand();

		RemoveAssigneeResponse response = assigneeCommandService.removeAssignee(
			code,
			issueKey,
			command,
			loginMemberId
		);

		return ApiResponse.ok("Assignee removed.", response);
	}
}
