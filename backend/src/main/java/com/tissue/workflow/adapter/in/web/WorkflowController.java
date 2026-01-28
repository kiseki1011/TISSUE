package com.tissue.workflow.adapter.in.web;

import com.tissue.project.adapter.in.web.resolver.CurrentProjectMember;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.adapter.in.web.request.CreateWorkflowRequest;
import com.tissue.workflow.adapter.in.web.request.ReplaceWorkflowGraphRequest;
import com.tissue.workflow.adapter.in.web.request.UpdateStateRequest;
import com.tissue.workflow.adapter.in.web.request.UpdateTransitionRequest;
import com.tissue.workflow.adapter.in.web.request.UpdateWorkflowRequest;
import com.tissue.workflow.application.dto.request.DeleteWorkflowCommand;
import com.tissue.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.workflow.application.dto.response.WorkflowDetail;
import com.tissue.workflow.application.dto.response.WorkflowSummary;
import com.tissue.workflow.application.port.in.WorkflowCommandUseCase;
import com.tissue.workflow.application.port.in.WorkflowGraphReplaceUseCase;
import com.tissue.workflow.application.port.in.WorkflowQueryUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/workflows")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowCommandUseCase workflowCommandUseCase;
    private final WorkflowGraphReplaceUseCase workflowGraphReplaceUseCase;
    private final WorkflowQueryUseCase workflowQueryUseCase;

    @PostMapping
    public ResponseEntity<WorkflowCreateResponse> createWorkflow(
            @RequestBody @Valid CreateWorkflowRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(actorContext);
        WorkflowCreateResponse response = workflowCommandUseCase.create(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{workflowId}")
                .buildAndExpand(response.workflowId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{workflowId}/graph")
    public ResponseEntity<Void> replaceWorkflowGraph(
            @PathVariable Long workflowId,
            @RequestBody @Valid ReplaceWorkflowGraphRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(workflowId, actorContext);
        workflowGraphReplaceUseCase.replaceWorkflowGraph(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workflowId}")
    public ResponseEntity<Void> updateWorkflow(
            @PathVariable Long workflowId,
            @RequestBody @Valid UpdateWorkflowRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(workflowId, actorContext);
        workflowCommandUseCase.update(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workflowId}")
    public ResponseEntity<Void> archiveWorkflow(
            @PathVariable Long workflowId, @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = new DeleteWorkflowCommand(workflowId, actorContext);
        workflowCommandUseCase.delete(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workflowId}/states/{stateId}")
    public ResponseEntity<WorkflowCreateResponse> updateWorkflowState(
            @PathVariable Long workflowId,
            @PathVariable Long stateId,
            @RequestBody @Valid UpdateStateRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(workflowId, stateId, actorContext);
        workflowCommandUseCase.updateState(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workflowId}/transitions/{transitionId}")
    public ResponseEntity<WorkflowCreateResponse> updateWorkflowTransition(
            @PathVariable Long workflowId,
            @PathVariable Long transitionId,
            @RequestBody @Valid UpdateTransitionRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand(workflowId, transitionId, actorContext);
        workflowCommandUseCase.updateTransition(command);

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<WorkflowSummary>> getWorkflows(@CurrentProjectMember ProjectMemberContext actorContext) {

        List<WorkflowSummary> workflows = workflowQueryUseCase.getWorkflows(actorContext);
        return ResponseEntity.ok(workflows);
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowDetail> getWorkflowDetail(
            @PathVariable Long workflowId, @CurrentProjectMember ProjectMemberContext actorContext) {

        WorkflowDetail detail = workflowQueryUseCase.getWorkflowDetail(workflowId, actorContext);
        return ResponseEntity.ok(detail);
    }
}
