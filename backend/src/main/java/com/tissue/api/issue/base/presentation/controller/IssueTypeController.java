package com.tissue.api.issue.base.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.base.application.service.IssueTypeService;
import com.tissue.api.issue.base.presentation.dto.request.CreateIssueFieldRequest;
import com.tissue.api.issue.base.presentation.dto.request.CreateIssueTypeRequest;
import com.tissue.api.issue.base.presentation.dto.request.UpdateIssueTypeRequest;
import com.tissue.api.issue.base.presentation.dto.response.IssueFieldResponse;
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
	 * TODO
	 *  - update custom issue type
	 *    - do not allow change of HierarchyLevel, Workflow
	 *  - delete custom issue type
	 *    - do not allow deletion if there are issues using the specific type
	 *  - prevent deletion of default system issue types
	 *  <p>
	 *  - update custom issue field
	 *    - do not allow to change the type of the field
	 *  - delete custom issue field
	 *    - do not allow deletion if there is a value using the specific field
	 *  - prevent deletion of default system issue fields
	 *  <p>
	 *  - refactor to use IssueFieldService
	 *  <p>
	 * TODO(In Consideration)
	 *  - HierarchyLevel update using validation
	 *  or increase/decrease HierarchyLevel of the whole IssueTypes by 1
	 *  - Workflow update using validation
	 *  or provide Issue migration
	 */
	private final IssueTypeService issueTypeService;

	@PostMapping
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueTypeResponse>> createIssueType(
		@PathVariable String workspaceKey,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid CreateIssueTypeRequest request
	) {
		IssueTypeResponse response = issueTypeService.createIssueType(request.toCommand(workspaceKey));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Custom issue type created.", response));
	}

	// TODO: Do not allow to change HierachyLevel, Workflow
	@PatchMapping("/{issueTypeKey}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueTypeResponse> updateIssueType(
		@PathVariable String workspaceKey,
		@PathVariable String issueTypeKey,
		@CurrentMember MemberUserDetails userDetails,
		@RequestBody @Valid UpdateIssueTypeRequest request
	) {
		IssueTypeResponse response = issueTypeService.updateIssueType(request.toCommand(workspaceKey, issueTypeKey));
		return ApiResponse.ok("Custom issue type updated.", response);
	}

	// TODO: Do not allow deletion if there are issues using the specific type
	// TODO: Prevent deletion of system issue type
	@DeleteMapping("/{issueTypeKey}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<Void> deleteIssueType(
		@PathVariable String workspaceKey,
		@PathVariable String issueTypeKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		issueTypeService.deleteIssueType(workspaceKey, issueTypeKey);
		return ApiResponse.okWithNoContent("Custom issue type deleted.");
	}

	// TODO: Consider making and using IssueFieldService
	@PostMapping("/{issueTypeKey}/fields")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueFieldResponse>> createField(
		@PathVariable String workspaceKey,
		@PathVariable String issueTypeKey,
		@RequestBody @Valid CreateIssueFieldRequest request
	) {
		IssueFieldResponse response = issueTypeService.createIssueField(request.toCommand(workspaceKey, issueTypeKey));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Custom issue field created.", response));
	}

	// TODO: Do not allow to change the type of the field
	// TODO: Consider separating the allowedOptions update to a different API
	//  Instead of replacing the whole allowedOptions field, consider adding or removing the item on the list
	// @PatchMapping("/{issueTypeKey}/fields/{issueFieldKey}")
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// public ApiResponse<IssueFieldResponse> updateField(
	// 	@PathVariable String workspaceKey,
	// 	@PathVariable String issueTypeKey,
	// 	@PathVariable String issueFieldKey,
	// 	@RequestBody @Valid UpdateIssueFieldRequest request
	// ) {
	// 	IssueFieldResponse response = issueTypeService.updateIssueField(request.toCommand(workspaceKey, issueTypeKey));
	// 	return ApiResponse.ok("Custom issue field updated.", response);
	// }

	// TODO: Do not allow deletion if there is a value using the specific field
	// TODO: Prevent deletion of default system issue fields
	// @DeleteMapping("/{issueTypeKey}/fields/{issueFieldKey}")
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// public ApiResponse<IssueFieldResponse> deleteField(
	// 	@PathVariable String workspaceKey,
	// 	@PathVariable String issueTypeKey,
	// 	@PathVariable String issueFieldKey
	// ) {
	// 	IssueFieldResponse response = issueTypeService.deleteIssueField(request.toCommand(workspaceKey, issueTypeKey));
	// 	return ApiResponse.ok("Custom issue field deleted.", response);
	// }
}
