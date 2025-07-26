package com.tissue.api.issue.presentation.controller.command;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.application.service.command.IssueTypeService;
import com.tissue.api.issue.presentation.controller.dto.request.CreateIssueFieldRequest;
import com.tissue.api.issue.presentation.controller.dto.request.CreateIssueTypeRequest;
import com.tissue.api.issue.presentation.controller.dto.response.IssueFieldResponse;
import com.tissue.api.issue.presentation.controller.dto.response.IssueTypeResponse;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceCode}/issue-types")
@RequiredArgsConstructor
public class IssueTypeController {

	/**
	 * TODO
	 *  - create custom issue type(IssueTypeDefinition)
	 *  - update custom issue type
	 *  - delete custom issue type
	 *  - prevent deletion of default system issue types
	 *  <p>
	 *  - create custom issue field(IssueFieldDefinition)
	 *  - update custom issue field
	 *  - delete custom issue field
	 *  - prevent deletion of default system issue fields
	 */

	private final IssueTypeService issueTypeService;

	@PostMapping
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueTypeResponse>> createIssueType(
		@PathVariable String workspaceCode,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid CreateIssueTypeRequest request
	) {
		IssueTypeResponse response = issueTypeService.createIssueType(request.toCommand(workspaceCode));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Custom issue type created.", response));
	}

	@PostMapping("/{issueTypeKey}/fields")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueFieldResponse>> createField(
		@PathVariable String workspaceCode,
		@PathVariable String issueTypeKey,
		@RequestBody @Valid CreateIssueFieldRequest request
	) {
		IssueFieldResponse response = issueTypeService.createIssueField(request.toCommand(workspaceCode, issueTypeKey));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Custom issue field created.", response));
	}
}
