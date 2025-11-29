package com.tissue.api.sprint.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.sprint.application.dto.request.AddSprintIssuesCommand;
import com.tissue.api.sprint.application.dto.request.CompleteSprintCommand;
import com.tissue.api.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.api.sprint.application.dto.request.RemoveSprintIssuesCommand;
import com.tissue.api.sprint.application.dto.response.SprintCommandResult;
import com.tissue.api.sprint.application.service.command.SprintCommandService;
import com.tissue.api.sprint.application.service.query.SprintQueryService;
import com.tissue.api.sprint.presentation.dto.request.AddSprintIssuesRequest;
import com.tissue.api.sprint.presentation.dto.request.CreateSprintRequest;
import com.tissue.api.sprint.presentation.dto.request.MigrateIssuesRequest;
import com.tissue.api.sprint.presentation.dto.request.RemoveSprintIssuesRequest;
import com.tissue.api.sprint.presentation.dto.request.StartSprintRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/sprints")
public class SprintController {

	private final SprintCommandService sprintCommandService;
	private final SprintQueryService sprintQueryService;

	@PostMapping
	public ResponseEntity<SprintCommandResult> createSprint(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid CreateSprintRequest request
	) {
		SprintCommandResult response = sprintCommandService.createSprint(
			request.toCommand(workspaceKey, projectKey)
		);
		return ResponseEntity.status(HttpStatus.CREATED)
			.body(response);
	}

	@PatchMapping("/{sprintId}")
	public ResponseEntity<SprintCommandResult> updateSprint(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long sprintId,
		@RequestBody @Valid UpdateSprintRequest request
	) {
		SprintCommandResult response = sprintCommandService.updateSprint(
			request.toCommand(workspaceKey, projectKey, sprintId)
		);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{sprintId}/start")
	public ResponseEntity<SprintCommandResult> startSprint(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long sprintId,
		@RequestBody @Valid StartSprintRequest request
	) {
		SprintCommandResult response = sprintCommandService.start(
			request.toCommand(workspaceKey, projectKey, sprintId)
		);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{sprintId}/complete")
	public ResponseEntity<SprintCommandResult> completeSprint(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long sprintId
	) {
		SprintCommandResult response = sprintCommandService.complete(
			new CompleteSprintCommand(workspaceKey, projectKey, sprintId)
		);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{sprintId}/issues")
	public ResponseEntity<SprintCommandResult> addIssues(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long sprintId,
		@RequestBody @Valid AddSprintIssuesRequest request
	) {
		SprintCommandResult response = sprintCommandService.addIssues(
			new AddSprintIssuesCommand(workspaceKey, projectKey, sprintId, request.issueKeys())
		);
		return ResponseEntity.ok(response);
	}

	@PostMapping("/{sprintId}/issues/migrate")
	public ResponseEntity<SprintCommandResult> migrateIncompleteIssues(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long sprintId,
		@RequestBody @Valid MigrateIssuesRequest request
	) {
		SprintCommandResult response = sprintCommandService.migrateIssues(
			MigrateSprintIssuesCommand.builder()
				.workspaceKey(workspaceKey)
				.projectKey(projectKey)
				.originalSprintId(sprintId)
				.newSprintId(request.newSprintId())
				.issueKeys(request.issueKeys())
				.build()
		);
		return ResponseEntity.ok(response);
	}

	@DeleteMapping("/{sprintId}/issues")
	public ResponseEntity<SprintCommandResult> removeIssue(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long sprintId,
		@RequestBody @Valid RemoveSprintIssuesRequest request
	) {
		SprintCommandResult response = sprintCommandService.removeIssues(
			new RemoveSprintIssuesCommand(workspaceKey, projectKey, sprintId, request.issueKeys())
		);
		return ResponseEntity.ok(response);
	}
}
