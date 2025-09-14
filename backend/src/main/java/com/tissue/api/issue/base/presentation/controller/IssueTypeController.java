package com.tissue.api.issue.base.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.base.application.service.IssueTypeService;
import com.tissue.api.issue.base.presentation.dto.request.CreateIssueTypeRequest;
import com.tissue.api.issue.base.presentation.dto.request.PatchIssueTypeRequest;
import com.tissue.api.issue.base.presentation.dto.request.RenameIssueTypeRequest;
import com.tissue.api.issue.base.presentation.dto.response.IssueTypeResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issue-types")
@RequiredArgsConstructor
public class IssueTypeController {

	/**
	 * TODO(In Consideration)
	 *  - HierarchyLevel update using validation
	 *  or increase/decrease HierarchyLevel of the whole IssueTypes by 1
	 *  - Workflow update using validation or provide Issue migration
	 */
	private final IssueTypeService issueTypeService;

	@PostMapping
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueTypeResponse>> create(
		@PathVariable String workspaceKey,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid CreateIssueTypeRequest request
	) {
		IssueTypeResponse response = issueTypeService.create(request.toCommand(workspaceKey));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Issue type created.", response));
	}

	@PutMapping("/{issueTypeKey}/rename")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueTypeResponse> rename(
		@PathVariable String workspaceKey,
		@PathVariable String issueTypeKey,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid RenameIssueTypeRequest request
	) {
		IssueTypeResponse response = issueTypeService.rename(request.toCommand(workspaceKey, issueTypeKey));
		return ApiResponse.ok("Issue type renamed.", response);
	}

	// Don't allow HierachyLevel, Workflow update
	@PatchMapping("/{issueTypeKey}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueTypeResponse> update(
		@PathVariable String workspaceKey,
		@PathVariable String issueTypeKey,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid PatchIssueTypeRequest request
	) {
		IssueTypeResponse response = issueTypeService.update(request.toCommand(workspaceKey, issueTypeKey));
		return ApiResponse.ok("Issue type updated.", response);
	}

	@DeleteMapping("/{issueTypeKey}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<Void> softDelete(
		@PathVariable String workspaceKey,
		@PathVariable String issueTypeKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		issueTypeService.softDelete(workspaceKey, issueTypeKey);
		return ApiResponse.okWithNoContent("Issue type deleted.");
	}
}
