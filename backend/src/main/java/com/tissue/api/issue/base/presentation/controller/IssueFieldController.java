package com.tissue.api.issue.base.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.base.application.dto.DeleteIssueFieldCommand;
import com.tissue.api.issue.base.application.service.IssueFieldService;
import com.tissue.api.issue.base.presentation.dto.request.AddOptionRequest;
import com.tissue.api.issue.base.presentation.dto.request.CreateIssueFieldRequest;
import com.tissue.api.issue.base.presentation.dto.request.PatchIssueFieldRequest;
import com.tissue.api.issue.base.presentation.dto.request.RenameOptionRequest;
import com.tissue.api.issue.base.presentation.dto.request.ReorderOptionsRequest;
import com.tissue.api.issue.base.presentation.dto.response.IssueFieldResponse;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issuetypes/{issueTypeId}")
@RequiredArgsConstructor
public class IssueFieldController {

	/**
	 * TODO
	 *  - add(create) field option -> allow batch creation?
	 *  - delete field option -> allow batch delete?
	 */
	private final IssueFieldService issueFieldService;

	@PostMapping("/fields")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueFieldResponse>> createIssueField(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@RequestBody @Valid CreateIssueFieldRequest request
	) {
		IssueFieldResponse response = issueFieldService.create(
			request.toCommand(workspaceKey, issueTypeId));

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Issue field created.", response));
	}

	@PutMapping("/fields/{id}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueFieldResponse> patchIssueField(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@RequestBody @Valid PatchIssueFieldRequest request
	) {
		IssueFieldResponse response = issueFieldService.patch(
			request.toCommand(workspaceKey, issueTypeId, id));

		return ApiResponse.ok("Issue field updated.", response);
	}

	@DeleteMapping("/fields/{id}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueFieldResponse> softDeleteIssueField(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id
	) {
		IssueFieldResponse response = issueFieldService.softDelete(
			new DeleteIssueFieldCommand(workspaceKey, issueTypeId, id));

		return ApiResponse.ok("Issue field deleted.", response);
	}

	@PostMapping("/fields/{id}/options")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueFieldResponse>> addEnumFieldOption(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@RequestBody @Valid AddOptionRequest request
	) {
		IssueFieldResponse response = issueFieldService.addOption(
			request.toCommand(workspaceKey, issueTypeId, id));

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Option for ENUM type issue field added.", response));
	}

	@PutMapping("/fields/{id}/options/{optionId}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueFieldResponse>> renameEnumFieldOption(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@PathVariable Long optionId,
		@RequestBody @Valid RenameOptionRequest request
	) {
		IssueFieldResponse response = issueFieldService.renameOption(
			request.toCommand(workspaceKey, issueTypeId, id, optionId));

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Option for ENUM type issue field renamed.", response));
	}

	@PutMapping("/fields/{id}/options")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueFieldResponse>> reorderEnumFieldOptions(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@RequestBody @Valid ReorderOptionsRequest request
	) {
		IssueFieldResponse response = issueFieldService.reorderOptions(
			request.toCommand(workspaceKey, issueTypeId, id));

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Options for ENUM type issue field reordered.", response));
	}

	@DeleteMapping("/fields/{id}/options/{optionId}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueFieldResponse>> softDeleteEnumFieldOption(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@PathVariable Long optionId
	) {
		IssueFieldResponse response = issueFieldService.softDeleteOption(workspaceKey, issueTypeId, id, optionId);

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Option for ENUM type issue field deleted.", response));
	}
}
