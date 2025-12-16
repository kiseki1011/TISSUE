package com.tissue.sprint.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.sprint.application.dto.request.AddSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.CompleteSprintCommand;
import com.tissue.sprint.application.dto.request.GetSprintDetailQuery;
import com.tissue.sprint.application.dto.request.GetSprintIssueKeysQuery;
import com.tissue.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.RemoveSprintIssuesCommand;
import com.tissue.sprint.application.dto.response.SprintCommandResult;
import com.tissue.sprint.application.dto.response.SprintDetail;
import com.tissue.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.sprint.application.port.in.SprintCommandUseCase;
import com.tissue.sprint.application.port.in.SprintQueryUseCase;
import com.tissue.sprint.adapter.in.web.dto.request.AddSprintIssuesRequest;
import com.tissue.sprint.adapter.in.web.dto.request.CreateSprintRequest;
import com.tissue.sprint.adapter.in.web.dto.request.MigrateIssuesRequest;
import com.tissue.sprint.adapter.in.web.dto.request.RemoveSprintIssuesRequest;
import com.tissue.sprint.adapter.in.web.dto.request.StartSprintRequest;
import com.tissue.sprint.adapter.in.web.dto.request.UpdateSprintRequest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/sprints")
public class SprintController {

	private final SprintCommandUseCase sprintCommandUseCase;
	private final SprintQueryUseCase sprintQueryUseCase;

	@PostMapping
	public ResponseEntity<SprintCommandResult> createSprint(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid CreateSprintRequest request
	) {
		SprintCommandResult response = sprintCommandUseCase.createSprint(
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
		SprintCommandResult response = sprintCommandUseCase.updateSprint(
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
		SprintCommandResult response = sprintCommandUseCase.start(
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
		SprintCommandResult response = sprintCommandUseCase.complete(
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
		SprintCommandResult response = sprintCommandUseCase.addIssues(
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
		SprintCommandResult response = sprintCommandUseCase.migrateIssues(
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
		SprintCommandResult response = sprintCommandUseCase.removeIssues(
			new RemoveSprintIssuesCommand(workspaceKey, projectKey, sprintId, request.issueKeys())
		);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{sprintId}")
	public ResponseEntity<SprintDetail> getSprintDetail(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long sprintId
	) {
		SprintDetail response = sprintQueryUseCase.getSprintDetail(
			new GetSprintDetailQuery(workspaceKey, projectKey, sprintId)
		);
		return ResponseEntity.ok(response);
	}

	@GetMapping("/{sprintId}/issues")
	public ResponseEntity<SprintIssueKeys> getSprintIssues(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long sprintId
	) {
		SprintIssueKeys response = sprintQueryUseCase.getSprintIssueKeys(
			new GetSprintIssueKeysQuery(workspaceKey, projectKey, sprintId)
		);
		return ResponseEntity.ok(response);
	}
}
