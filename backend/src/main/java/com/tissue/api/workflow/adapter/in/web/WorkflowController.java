package com.tissue.api.workflow.adapter.in.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.workflow.adapter.in.web.dto.request.CreateWorkflowRequest;
import com.tissue.api.workflow.adapter.in.web.dto.request.ReplaceWorkflowGraphRequest;
import com.tissue.api.workflow.adapter.in.web.dto.request.UpdateStateRequest;
import com.tissue.api.workflow.adapter.in.web.dto.request.UpdateTransitionRequest;
import com.tissue.api.workflow.adapter.in.web.dto.request.UpdateWorkflowRequest;
import com.tissue.api.workflow.application.dto.request.ArchiveWorkflowCommand;
import com.tissue.api.workflow.application.dto.response.WorkflowResponse;
import com.tissue.api.workflow.application.port.in.WorkflowCommandUseCase;
import com.tissue.api.workflow.application.port.in.WorkflowGraphReplaceUseCase;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/workflows")
public class WorkflowController {

	private final WorkflowCommandUseCase workflowCommandUseCase;
	private final WorkflowGraphReplaceUseCase workflowGraphReplaceUseCase;

	@PostMapping
	public ResponseEntity<WorkflowResponse> createWorkflow(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@RequestBody @Valid CreateWorkflowRequest request
	) {
		WorkflowResponse response = workflowCommandUseCase.create(request.toCommand(workspaceKey));

		// TODO: created 사용
		return ResponseEntity.status(HttpStatus.CREATED)
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
		workflowCommandUseCase.archive(new ArchiveWorkflowCommand(workspaceKey, projectKey, workflowId));
		return ResponseEntity.noContent().build();
	}

	@PatchMapping("/{workflowId}/states/{stateId}")
	public ResponseEntity<WorkflowResponse> updateWorkflowState(
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
	public ResponseEntity<WorkflowResponse> updateWorkflowTransition(
		@PathVariable String workspaceKey,
		@PathVariable String projectKey,
		@PathVariable Long workflowId,
		@PathVariable Long transitionId,
		@RequestBody @Valid UpdateTransitionRequest req
	) {
		workflowCommandUseCase.updateTransition(req.toCommand(workspaceKey, projectKey, workflowId, transitionId));
		return ResponseEntity.noContent().build();
	}
}
