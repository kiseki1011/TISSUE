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
import com.tissue.api.issue.base.application.service.IssueFieldService;
import com.tissue.api.issue.base.presentation.dto.request.AddOptionRequest;
import com.tissue.api.issue.base.presentation.dto.request.CreateIssueFieldRequest;
import com.tissue.api.issue.base.presentation.dto.request.PatchIssueFieldRequest;
import com.tissue.api.issue.base.presentation.dto.request.RenameIssueFieldRequest;
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
		@RequestBody @Valid CreateIssueFieldRequest req
	) {
		IssueFieldResponse res = issueFieldService.create(req.toCommand(workspaceKey, issueTypeId));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Issue field created.", res));
	}

	@PutMapping("/fields/{id}/rename")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueFieldResponse> renameIssueField(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@RequestBody @Valid RenameIssueFieldRequest req
	) {
		IssueFieldResponse res = issueFieldService.rename(req.toCommand(workspaceKey, issueTypeId, id));
		return ApiResponse.ok("Issue field renamed.", res);
	}

	@PatchMapping("/fields/{id}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueFieldResponse> patchIssueField(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@RequestBody @Valid PatchIssueFieldRequest req
	) {
		IssueFieldResponse res = issueFieldService.patch(req.toCommand(workspaceKey, issueTypeId, id));
		return ApiResponse.ok("Issue field updated.", res);
	}

	@DeleteMapping("/fields/{id}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueFieldResponse> softDeleteIssueField(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id
	) {
		IssueFieldResponse res = issueFieldService.softDelete(workspaceKey, issueTypeId, id);
		return ApiResponse.ok("Issue field archived.", res);
	}

	@PostMapping("/fields/{id}/options")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ApiResponse<IssueFieldResponse>> addEnumFieldOption(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@RequestBody @Valid AddOptionRequest req
	) {
		IssueFieldResponse res = issueFieldService.addOption(req.toCommand(workspaceKey, issueTypeId, id));
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ApiResponse.created("Option for ENUM type issue field added.", res));
	}

	@PutMapping("/fields/{id}/options/{optionId}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueFieldResponse> renameEnumFieldOption(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@PathVariable Long optionId,
		@RequestBody @Valid RenameOptionRequest req
	) {
		IssueFieldResponse res = issueFieldService.renameOption(req.toCommand(workspaceKey, issueTypeId, id, optionId));
		return ApiResponse.ok("Option for ENUM type issue field renamed.", res);
	}

	@PutMapping("/fields/{id}/options")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueFieldResponse> reorderEnumFieldOptions(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@RequestBody @Valid ReorderOptionsRequest req
	) {
		IssueFieldResponse res = issueFieldService.reorderOptions(req.toCommand(workspaceKey, issueTypeId, id));
		return ApiResponse.ok("Options for ENUM type issue field reordered.", res);
	}

	@DeleteMapping("/fields/{id}/options/{optionId}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ApiResponse<IssueFieldResponse> softDeleteEnumFieldOption(
		@PathVariable String workspaceKey,
		@PathVariable Long issueTypeId,
		@PathVariable Long id,
		@PathVariable Long optionId
	) {
		IssueFieldResponse res = issueFieldService.softDeleteOption(workspaceKey, issueTypeId, id, optionId);
		return ApiResponse.ok("Option for ENUM type issue field archived.", res);
	}
}
