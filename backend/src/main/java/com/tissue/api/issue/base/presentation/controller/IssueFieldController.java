package com.tissue.api.issue.base.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.base.application.service.IssueFieldService;
import com.tissue.api.issue.base.presentation.dto.request.CreateIssueFieldRequest;
import com.tissue.api.issue.base.presentation.dto.request.UpdateIssueFieldRequest;
import com.tissue.api.issue.base.presentation.dto.response.IssueFieldResponse;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issue-types")
@RequiredArgsConstructor
public class IssueFieldController {

	/**
	 * TODO
	 *  - update custom issue field
	 *    - do not allow to change the FieldType of the field
	 *  - delete custom issue field
	 *    - do not allow deletion if there is a value using the specific field?
	 *    - or allow the field deletion and delete the values of the field via cascade?
	 *  - make EnumFieldOption entity
	 *  - add(create) field option
	 *    - allow batch creation
	 *  - update field option if issue field type is ENUM
	 *    - allow batch update
	 *  - delete field option
	 *    - allow batch delete
	 */
	private final IssueFieldService issueFieldService;

	@PostMapping("/{issueTypeKey}/fields")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueFieldResponse>> createIssueField(
		@PathVariable String workspaceKey,
		@PathVariable String issueTypeKey,
		@RequestBody @Valid CreateIssueFieldRequest request
	) {
		IssueFieldResponse response = issueFieldService.create(request.toCommand(workspaceKey, issueTypeKey));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Issue field created.", response));
	}

	// TODO: Do not allow to change the type of the field
	// TODO: Instead of replacing the whole allowedOptions field, should I consider
	//  add/remove/update of the item on the allowedOptions
	@PutMapping("/{issueTypeKey}/fields/{issueFieldKey}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueFieldResponse> updateIssueFieldMetaData(
		@PathVariable String workspaceKey,
		@PathVariable String issueTypeKey,
		@PathVariable String issueFieldKey,
		@RequestBody @Valid UpdateIssueFieldRequest request
	) {
		IssueFieldResponse response = issueFieldService.updateMetaData(
			request.toCommand(workspaceKey, issueTypeKey, issueFieldKey)
		);
		return ApiResponse.ok("Issue field updated.", response);
	}

	// TODO: Do not allow deletion if there is a value using the specific field
	//  or should I just allow the field deletion and delete the values of the field via cascade?
	// @DeleteMapping("/{issueTypeKey}/fields/{issueFieldKey}")
	// @RoleRequired(role = WorkspaceRole.MEMBER)
	// public ApiResponse<IssueFieldResponse> deleteIssueField(
	// 	@PathVariable String workspaceKey,
	// 	@PathVariable String issueTypeKey,
	// 	@PathVariable String issueFieldKey
	// ) {
	// 	IssueFieldResponse response = issueTypeService.deleteIssueField(request.toCommand(workspaceKey, issueTypeKey));
	// 	return ApiResponse.ok("Custom issue field deleted.", response);
	// }
}
