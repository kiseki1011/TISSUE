package com.tissue.api.assignee.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.assignee.presentation.dto.response.AddAssigneeResponse;
import com.tissue.api.assignee.presentation.dto.response.RemoveAssigneeResponse;
import com.tissue.api.assignee.service.command.AssigneeCommandService;
import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.resolver.CurrentWorkspaceMember;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{code}/issues/{issueKey}/assignees")
public class AssigneeController {

	private final AssigneeCommandService assigneeCommandService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{workspaceMemberId}")
	public ApiResponse<AddAssigneeResponse> addAssignee(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable Long workspaceMemberId,
		@CurrentWorkspaceMember Long currentWorkspaceMemberId
	) {
		AddAssigneeResponse response = assigneeCommandService.addAssignee(
			code,
			issueKey,
			workspaceMemberId,
			currentWorkspaceMemberId
		);

		return ApiResponse.ok("Assignee added.", response);
	}

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{workspaceMemberId}")
	public ApiResponse<RemoveAssigneeResponse> removeAssignee(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable Long workspaceMemberId,
		@CurrentWorkspaceMember Long requesterId
	) {
		RemoveAssigneeResponse response = assigneeCommandService.removeAssignee(
			code,
			issueKey,
			workspaceMemberId,
			requesterId
		);

		return ApiResponse.ok("Assignee removed.", response);
	}
}
