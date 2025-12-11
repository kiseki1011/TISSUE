package com.tissue.api.workflow.adapter.in.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.tissue.api.workflow.adapter.in.web.dto.request.CreateWorkflowRequest;
import com.tissue.api.workflow.adapter.in.web.dto.request.ReplaceWorkflowGraphRequest;
import com.tissue.api.workflow.adapter.in.web.dto.request.UpdateStateRequest;
import com.tissue.api.workflow.adapter.in.web.dto.request.UpdateTransitionRequest;
import com.tissue.api.workflow.adapter.in.web.dto.request.UpdateWorkflowRequest;
import com.tissue.api.workflow.application.dto.request.DeleteWorkflowCommand;
import com.tissue.api.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.api.workflow.application.dto.response.WorkflowDetail;
import com.tissue.api.workflow.application.dto.response.WorkflowSummary;
import com.tissue.api.workflow.application.port.in.WorkflowCommandUseCase;
import com.tissue.api.workflow.application.port.in.WorkflowGraphReplaceUseCase;
import com.tissue.api.workflow.application.port.in.WorkflowQueryUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/workflows")
public class WorkflowController {

	private final WorkflowCommandUseCase workflowCommandUseCase;
	private final WorkflowGraphReplaceUseCase workflowGraphReplaceUseCase;
	private final WorkflowQueryUseCase workflowQueryUseCase;

	@PostMapping
	public ResponseEntity<WorkflowCreateResponse> createWorkflow(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid CreateWorkflowRequest request
	) {
		WorkflowCreateResponse response = workflowCommandUseCase.create(request.toCommand(workspaceKey, projectKey));

		URI location = ServletUriComponentsBuilder
			.fromCurrentRequest()
			.path("/{workflowId}")
			.buildAndExpand(response.workflowId())
			.toUri();

		return ResponseEntity.created(location)
			.body(response);
	}

	@PatchMapping("/{workflowId}/graph")
	public ResponseEntity<Void> replaceWorkflowGraph(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long workflowId,
		@RequestBody @Valid ReplaceWorkflowGraphRequest request
	) {
		workflowGraphReplaceUseCase.replaceWorkflowGraph(request.toCommand(workspaceKey, projectKey, workflowId));
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{workflowId}")
	public ResponseEntity<Void> updateWorkflow(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long workflowId,
		@RequestBody @Valid UpdateWorkflowRequest request
	) {
		workflowCommandUseCase.update(request.toCommand(workspaceKey, projectKey, workflowId));
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping("/{workflowId}")
	public ResponseEntity<Void> archiveWorkflow(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long workflowId
	) {
		workflowCommandUseCase.delete(new DeleteWorkflowCommand(workspaceKey, projectKey, workflowId));
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{workflowId}/states/{stateId}")
	public ResponseEntity<WorkflowCreateResponse> updateWorkflowState(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long workflowId,
		@PathVariable Long stateId,
		@RequestBody @Valid UpdateStateRequest req
	) {
		workflowCommandUseCase.updateState(req.toCommand(workspaceKey, projectKey, workflowId, stateId));
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{workflowId}/transitions/{transitionId}")
	public ResponseEntity<WorkflowCreateResponse> updateWorkflowTransition(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long workflowId,
		@PathVariable Long transitionId,
		@RequestBody @Valid UpdateTransitionRequest req
	) {
		workflowCommandUseCase.updateTransition(req.toCommand(workspaceKey, projectKey, workflowId, transitionId));
		return ResponseEntity.noContent().build();
	}

	@GetMapping
	public ResponseEntity<List<WorkflowSummary>> getWorkflows(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestParam(required = false, defaultValue = "false") boolean includeArchived
	) {
		List<WorkflowSummary> workflows = workflowQueryUseCase.getWorkflows(workspaceKey, projectKey, includeArchived);
		return ResponseEntity.ok(workflows);
	}

	@GetMapping("/{workflowId}")
	public ResponseEntity<WorkflowDetail> getWorkflowDetail(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long workflowId
	) {
		WorkflowDetail detail = workflowQueryUseCase.getWorkflowDetail(workspaceKey, projectKey, workflowId);
		return ResponseEntity.ok(detail);
	}
}
