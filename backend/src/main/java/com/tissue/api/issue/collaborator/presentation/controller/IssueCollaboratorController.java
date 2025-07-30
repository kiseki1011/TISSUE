package com.tissue.api.issue.collaborator.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.collaborator.application.dto.AddAssigneeCommand;
import com.tissue.api.issue.collaborator.application.dto.RemoveAssigneeCommand;
import com.tissue.api.issue.collaborator.application.service.IssueCollaboratorService;
import com.tissue.api.issue.collaborator.presentation.dto.request.AddAssigneeRequest;
import com.tissue.api.issue.collaborator.presentation.dto.request.RemoveAssigneeRequest;
import com.tissue.api.issue.collaborator.presentation.dto.response.IssueAssigneeResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// TODO: Move to IssueAssociateController
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{code}/issues/{issueKey}/assignees")
public class IssueCollaboratorController {

	private final IssueCollaboratorService issueCollaboratorService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ApiResponse<IssueAssigneeResponse> addAssignee(
		@PathVariable String code,
		@PathVariable String issueKey,
		@RequestBody @Valid AddAssigneeRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		AddAssigneeCommand command = request.toCommand();

		IssueAssigneeResponse response = issueCollaboratorService.addAssignee(
			code,
			issueKey,
			command,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Assignee added.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping
	public ApiResponse<IssueAssigneeResponse> removeAssignee(
		@PathVariable String code,
		@PathVariable String issueKey,
		@RequestBody @Valid RemoveAssigneeRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		RemoveAssigneeCommand command = request.toCommand();

		IssueAssigneeResponse response = issueCollaboratorService.removeAssignee(
			code,
			issueKey,
			command,
			userDetails.getMemberId()
		);

		return ApiResponse.ok("Assignee removed.", response);
	}
}
