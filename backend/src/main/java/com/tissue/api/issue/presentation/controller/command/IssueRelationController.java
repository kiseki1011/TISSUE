package com.tissue.api.issue.presentation.controller.command;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.service.command.IssueRelationCommandService;
import com.tissue.api.issue.presentation.controller.dto.request.CreateIssueRelationRequest;
import com.tissue.api.issue.presentation.controller.dto.response.IssueRelationResponse;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authentication.resolver.ResolveLoginMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{code}/issues/{issueKey}/relations")
public class IssueRelationController {

	private final IssueRelationCommandService issueRelationCommandService;

	@LoginRequired
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping("/{targetIssueKey}")
	public ApiResponse<IssueRelationResponse> createRelation(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable String targetIssueKey,
		@ResolveLoginMember Long loginMemberId,
		@RequestBody @Valid CreateIssueRelationRequest request
	) {
		IssueRelationResponse response = issueRelationCommandService.createRelation(
			code,
			issueKey,
			targetIssueKey,
			loginMemberId,
			request
		);

		return ApiResponse.ok("Issue relation created.", response);
	}

	@LoginRequired
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{targetIssueKey}")
	public ApiResponse<Void> removeRelation(
		@PathVariable String code,
		@PathVariable String issueKey,
		@PathVariable String targetIssueKey,
		@ResolveLoginMember Long loginMemberId
	) {
		issueRelationCommandService.removeRelation(
			code,
			issueKey,
			targetIssueKey,
			loginMemberId
		);

		return ApiResponse.okWithNoContent("Issue relation removed.");
	}
}
