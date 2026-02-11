package com.tissue.workflow.web;

import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import com.tissue.feature.workflow.application.port.usecase.WorkflowCommandUseCase;
import com.tissue.feature.workflow.application.port.usecase.WorkflowGraphReplaceUseCase;
import com.tissue.feature.workflow.application.port.usecase.WorkflowQueryUseCase;
import com.tissue.project.web.resolver.CurrentProjectMember;
import com.tissue.workflow.web.request.CreateWorkflowRequest;
import com.tissue.workflow.web.request.ReplaceWorkflowGraphRequest;
import com.tissue.workflow.web.request.UpdateStateRequest;
import com.tissue.workflow.web.request.UpdateTransitionRequest;
import com.tissue.workflow.web.request.UpdateWorkflowRequest;
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

        var command = request.toCommand();
        WorkflowCreateResponse response = workflowCommandUseCase.create(command, actorContext);

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

        var command = request.toCommand();
        workflowGraphReplaceUseCase.replaceWorkflowGraph(workflowId, command, actorContext);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workflowId}")
    public ResponseEntity<Void> updateWorkflow(
            @PathVariable Long workflowId,
            @RequestBody @Valid UpdateWorkflowRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand();
        workflowCommandUseCase.update(workflowId, command, actorContext);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workflowId}")
    public ResponseEntity<Void> archiveWorkflow(
            @PathVariable Long workflowId, @CurrentProjectMember ProjectMemberContext actorContext) {

        workflowCommandUseCase.delete(workflowId, actorContext);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workflowId}/states/{stateId}")
    public ResponseEntity<WorkflowCreateResponse> updateWorkflowState(
            @PathVariable Long workflowId,
            @PathVariable Long stateId,
            @RequestBody @Valid UpdateStateRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand();
        workflowCommandUseCase.updateState(workflowId, stateId, command, actorContext);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workflowId}/transitions/{transitionId}")
    public ResponseEntity<WorkflowCreateResponse> updateWorkflowTransition(
            @PathVariable Long workflowId,
            @PathVariable Long transitionId,
            @RequestBody @Valid UpdateTransitionRequest request,
            @CurrentProjectMember ProjectMemberContext actorContext) {

        var command = request.toCommand();
        workflowCommandUseCase.updateTransition(workflowId, transitionId, command, actorContext);

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
