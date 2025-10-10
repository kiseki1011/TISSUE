package com.tissue.api.issue.presentation.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.service.IssueRelationService;
import com.tissue.api.issue.presentation.dto.request.AddIssueRelationRequest;
import com.tissue.api.issue.presentation.dto.request.RemoveIssueRelationRequest;
import com.tissue.api.issue.presentation.dto.response.IssueRelationResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues/{issueKey}/relations")
public class IssueRelationController {

	private final IssueRelationService issueRelationService;

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@PostMapping
	public ApiResponse<IssueRelationResponse> addRelation(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid AddIssueRelationRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueRelationResponse response = issueRelationService.add(request.toCommand(workspaceKey, issueKey));

		return ApiResponse.ok("Issue relation created.", response);
	}

	@RoleRequired(role = WorkspaceRole.MEMBER)
	@DeleteMapping("/{relationId}")
	public ApiResponse<Void> removeRelation(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid RemoveIssueRelationRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		issueRelationService.remove(workspaceKey, issueKey, request.targetIssueKey());

		return ApiResponse.okWithNoContent("Issue relation removed.");
	}
}
