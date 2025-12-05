package com.tissue.api.issuetype.adapter.in.web;

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

import com.tissue.api.issuetype.adapter.in.dto.request.CreateIssueTypeRequest;
import com.tissue.api.issuetype.adapter.in.dto.request.RenameIssueTypeRequest;
import com.tissue.api.issuetype.adapter.in.dto.request.UpdateIssueTypeRequest;
import com.tissue.api.issuetype.application.dto.request.DeleteIssueTypeCommand;
import com.tissue.api.issuetype.application.dto.response.IssueTypeResponse;
import com.tissue.api.issuetype.application.service.IssueTypeService;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspace.domain.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/issuetypes")
@RequiredArgsConstructor
public class IssueTypeController {

	private final IssueTypeService issueTypeService;

	@PostMapping
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueTypeResponse> create(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid CreateIssueTypeRequest req
	) {
		IssueTypeResponse response = issueTypeService.create(
			req.toCommand(workspaceKey, projectKey)
		);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(response);
	}

	@PutMapping("/{id}/rename")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueTypeResponse> rename(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long id,
		@RequestBody @Valid RenameIssueTypeRequest request
	) {
		IssueTypeResponse response = issueTypeService.rename(
			request.toCommand(workspaceKey, projectKey, id)
		);
		return ResponseEntity.ok(response);
	}

	@PatchMapping("/{id}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueTypeResponse> update(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long id,
		@RequestBody @Valid UpdateIssueTypeRequest request
	) {
		IssueTypeResponse response = issueTypeService.update(
			request.toCommand(workspaceKey, projectKey, id)
		);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{id}")
	@RoleRequired(role = WorkspaceRole.MEMBER)
	public ResponseEntity<IssueTypeResponse> delete(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long id
	) {
		IssueTypeResponse response = issueTypeService.delete(
			new DeleteIssueTypeCommand(workspaceKey, projectKey, id)
		);
		return ResponseEntity.ok(response);
	}
}
