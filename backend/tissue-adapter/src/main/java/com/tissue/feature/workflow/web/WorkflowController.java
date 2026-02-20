package com.tissue.feature.workflow.web;

import com.tissue.feature.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import com.tissue.feature.workflow.application.port.usecase.WorkflowCommandUseCase;
import com.tissue.feature.workflow.application.port.usecase.WorkflowGraphReplaceUseCase;
import com.tissue.feature.workflow.application.port.usecase.WorkflowQueryUseCase;
import com.tissue.feature.workflow.web.request.ConfigureTransitionGuardsRequest;
import com.tissue.feature.workflow.web.request.CreateWorkflowRequest;
import com.tissue.feature.workflow.web.request.ReplaceWorkflowGraphRequest;
import com.tissue.feature.workflow.web.request.UpdateStateRequest;
import com.tissue.feature.workflow.web.request.UpdateTransitionRequest;
import com.tissue.feature.workflow.web.request.UpdateWorkflowRequest;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
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
import org.springframework.web.bind.annotation.PutMapping;
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
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid CreateWorkflowRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        WorkflowCreateResponse response = workflowCommandUseCase.create(
                ProjectIdentifier.of(workspaceKey, projectKey), command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{workflowId}")
                .buildAndExpand(response.workflowId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{workflowId}/graph")
    public ResponseEntity<Void> replaceWorkflowGraph(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long workflowId,
            @RequestBody @Valid ReplaceWorkflowGraphRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        workflowGraphReplaceUseCase.replaceWorkflowGraph(
                ProjectIdentifier.of(workspaceKey, projectKey), workflowId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workflowId}")
    public ResponseEntity<Void> updateWorkflow(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long workflowId,
            @RequestBody @Valid UpdateWorkflowRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        workflowCommandUseCase.update(
                ProjectIdentifier.of(workspaceKey, projectKey), workflowId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workflowId}")
    public ResponseEntity<Void> archiveWorkflow(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long workflowId,
            @CurrentMember MemberDetails memberDetails) {

        workflowCommandUseCase.delete(
                ProjectIdentifier.of(workspaceKey, projectKey), workflowId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workflowId}/states/{stateId}")
    public ResponseEntity<WorkflowCreateResponse> updateWorkflowState(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long workflowId,
            @PathVariable Long stateId,
            @RequestBody @Valid UpdateStateRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        workflowCommandUseCase.updateState(
                ProjectIdentifier.of(workspaceKey, projectKey),
                workflowId,
                stateId,
                command,
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workflowId}/transitions/{transitionId}")
    public ResponseEntity<WorkflowCreateResponse> updateWorkflowTransition(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long workflowId,
            @PathVariable Long transitionId,
            @RequestBody @Valid UpdateTransitionRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        workflowCommandUseCase.updateTransition(
                ProjectIdentifier.of(workspaceKey, projectKey),
                workflowId,
                transitionId,
                command,
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{workflowId}/transitions/{transitionId}/guards")
    public ResponseEntity<Void> configureTransitionGuards(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long workflowId,
            @PathVariable Long transitionId,
            @RequestBody @Valid ConfigureTransitionGuardsRequest request,
            @CurrentMember MemberDetails memberDetails) {

        workflowCommandUseCase.configureTransitionGuards(
                ProjectIdentifier.of(workspaceKey, projectKey),
                workflowId,
                transitionId,
                request.toCommand(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<WorkflowSummary>> getWorkflows(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {

        List<WorkflowSummary> workflows = workflowQueryUseCase.getWorkflows(
                ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());
        return ResponseEntity.ok(workflows);
    }

    @GetMapping("/{workflowId}")
    public ResponseEntity<WorkflowDetail> getWorkflowDetail(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long workflowId,
            @CurrentMember MemberDetails memberDetails) {

        WorkflowDetail detail = workflowQueryUseCase.getWorkflowDetail(
                ProjectIdentifier.of(workspaceKey, projectKey), workflowId, memberDetails.getMemberId());
        return ResponseEntity.ok(detail);
    }
}
