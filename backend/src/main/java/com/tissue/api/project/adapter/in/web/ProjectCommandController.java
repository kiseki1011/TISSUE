package com.tissue.api.project.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.project.adapter.in.web.dto.request.CreateProjectRequest;
import com.tissue.api.project.adapter.in.web.dto.request.UpdateProjectKeyRequest;
import com.tissue.api.project.adapter.in.web.dto.request.UpdateProjectRequest;
import com.tissue.api.project.application.dto.request.DeleteProjectCommand;
import com.tissue.api.project.application.dto.request.UpdateProjectKeyCommand;
import com.tissue.api.project.application.dto.response.ProjectCommandResult;
import com.tissue.api.project.application.port.in.ProjectCommandUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects")
public class ProjectCommandController {

	private final ProjectCommandUseCase projectCommandUseCase;

	@PostMapping
	public ResponseEntity<ProjectCommandResult> create(
		@PathVariable String workspaceKey,
		@RequestBody @Valid CreateProjectRequest request
	) {
		ProjectCommandResult response = projectCommandUseCase.create(request.toCommand(workspaceKey));

		return ResponseEntity.status(HttpStatus.CREATED)
			.body(response);
	}

	@PatchMapping("/{projectKey}")
	public ResponseEntity<ProjectCommandResult> update(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid UpdateProjectRequest request
	) {
		ProjectCommandResult response = projectCommandUseCase.update(request.toCommand(workspaceKey, projectKey));

		return ResponseEntity.ok(response);
	}

	// TODO: "key" 대신 "update-key"가 나으려나?
	@PatchMapping("/{projectKey}/key")
	public ResponseEntity<ProjectCommandResult> updateKey(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid UpdateProjectKeyRequest request
	) {
		ProjectCommandResult response = projectCommandUseCase.updateKey(
			new UpdateProjectKeyCommand(
				workspaceKey,
				projectKey,
				request.newKey()
			)
		);

		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{projectKey}")
	public ResponseEntity<ProjectCommandResult> delete(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey
	) {
		ProjectCommandResult response = projectCommandUseCase.delete(
			new DeleteProjectCommand(
				workspaceKey,
				projectKey
			)
		);

		return ResponseEntity.ok(response);
	}
}
