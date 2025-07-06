package com.tissue.api.issue.presentation.controller.command;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.dto.AddAssigneeCommand;
import com.tissue.api.issue.application.dto.RemoveAssigneeCommand;
import com.tissue.api.issue.application.service.command.IssueAssigneeCommandService;
import com.tissue.api.issue.presentation.controller.dto.request.AddAssigneeRequest;
import com.tissue.api.issue.presentation.controller.dto.request.RemoveAssigneeRequest;
import com.tissue.api.issue.presentation.controller.dto.response.IssueAssigneeResponse;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{code}/issues/{issueKey}/assignees")
public class IssueAssigneeController {

	private final IssueAssigneeCommandService issueAssigneeCommandService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ApiResponse<IssueAssigneeResponse> addAssignee(
		@PathVariable String code,
		@PathVariable String issueKey,
		@RequestBody @Valid AddAssigneeRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		AddAssigneeCommand command = request.toCommand();

		IssueAssigneeResponse response = issueAssigneeCommandService.addAssignee(
			code,
			issueKey,
			command,
			loginMemberId
		);

		return ApiResponse.ok("Assignee added.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping
	public ApiResponse<IssueAssigneeResponse> removeAssignee(
		@PathVariable String code,
		@PathVariable String issueKey,
		@RequestBody @Valid RemoveAssigneeRequest request,
		@ResolveLoginMember Long loginMemberId
	) {
		RemoveAssigneeCommand command = request.toCommand();

		IssueAssigneeResponse response = issueAssigneeCommandService.removeAssignee(
			code,
			issueKey,
			command,
			loginMemberId
		);

		return ApiResponse.ok("Assignee removed.", response);
	}
}
