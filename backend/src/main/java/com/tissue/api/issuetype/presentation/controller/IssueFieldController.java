package com.tissue.api.issuetype.presentation.controller;

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

import com.tissue.api.issuetype.application.dto.DeleteIssueFieldCommand;
import com.tissue.api.issuetype.application.dto.DeleteOptionCommand;
import com.tissue.api.issuetype.application.service.IssueFieldService;
import com.tissue.api.issuetype.presentation.dto.request.AddOptionRequest;
import com.tissue.api.issuetype.presentation.dto.request.CreateIssueFieldRequest;
import com.tissue.api.issuetype.presentation.dto.request.PatchIssueFieldRequest;
import com.tissue.api.issuetype.presentation.dto.request.RenameIssueFieldRequest;
import com.tissue.api.issuetype.presentation.dto.request.RenameOptionRequest;
import com.tissue.api.issuetype.presentation.dto.request.ReorderOptionsRequest;
import com.tissue.api.issuetype.presentation.dto.response.IssueFieldResponse;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/types/{typeId}/fields")
@RequiredArgsConstructor
public class IssueFieldController {

	private final IssueFieldService issueFieldService;

	@PostMapping
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueFieldResponse> create(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long typeId,
		@RequestBody @Valid CreateIssueFieldRequest request
	) {
		IssueFieldResponse response = issueFieldService.create(
			request.toCommand(workspaceKey, projectKey, typeId));

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(response);
	}

	@PutMapping("/{fieldId}/rename")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueFieldResponse> rename(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long typeId,
		@PathVariable Long fieldId,
		@RequestBody @Valid RenameIssueFieldRequest request
	) {
		IssueFieldResponse response = issueFieldService.rename(
			request.toCommand(workspaceKey, projectKey, typeId, fieldId));

		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{fieldId}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueFieldResponse> update(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long typeId,
		@PathVariable Long fieldId,
		@RequestBody @Valid PatchIssueFieldRequest request
	) {
		IssueFieldResponse response = issueFieldService.update(
			request.toCommand(workspaceKey, projectKey, typeId, fieldId));

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{fieldId}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueFieldResponse> deleteIssueField(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long typeId,
		@PathVariable Long fieldId
	) {
		IssueFieldResponse response = issueFieldService.delete(
			DeleteIssueFieldCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueTypeId(typeId)
				.issueFieldId(fieldId)
				.build()
		);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/{fieldId}/options")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<ResponseEntity<IssueFieldResponse>> addIssueFieldOption(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long typeId,
		@PathVariable Long fieldId,
		@RequestBody @Valid AddOptionRequest request
	) {
		IssueFieldResponse response = issueFieldService.addOption(
			request.toCommand(workspaceKey, projectKey, typeId, fieldId));

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(ResponseEntity.ok(response));
	}

	@PutMapping("/{fieldId}/options/{optionId}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueFieldResponse> renameIssueFieldOption(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long typeId,
		@PathVariable Long fieldId,
		@PathVariable Long optionId,
		@RequestBody @Valid RenameOptionRequest request
	) {
		IssueFieldResponse response = issueFieldService.renameOption(
			request.toCommand(workspaceKey, projectKey, typeId, fieldId, optionId));

		return ResponseEntity.ok(response);
	}

	@PutMapping("/{fieldId}/options")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueFieldResponse> reorderIssueFieldOptions(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long typeId,
		@PathVariable Long fieldId,
		@RequestBody @Valid ReorderOptionsRequest request
	) {
		IssueFieldResponse response = issueFieldService.reorderOptions(
			request.toCommand(workspaceKey, projectKey, typeId, fieldId));

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{fieldId}/options/{optionId}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueFieldResponse> deleteIssueFieldOption(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long typeId,
		@PathVariable Long fieldId,
		@PathVariable Long optionId
	) {
		IssueFieldResponse response = issueFieldService.deleteOption(
			DeleteOptionCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.issueTypeId(typeId)
				.issueFieldId(fieldId)
				.optionId(optionId)
				.build()
		);

		return ResponseEntity.ok(response);
	}
}
