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
import com.tissue.feature.workflow.web.request.UpdateWorkflowVcsSettingsRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Workflow")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowCommandUseCase workflowCommandUseCase;
    private final WorkflowGraphReplaceUseCase workflowGraphReplaceUseCase;
    private final WorkflowQueryUseCase workflowQueryUseCase;

    @Operation(summary = "Create workflow", description = """
                Create a new workflow with states and transitions.
                 Each state and transition must include a client-generated `tempKey`
                 that is unique within the request.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Workflow created"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request or invalid graph structure",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "409", description = "Workflow name already exists", content = @Content)
    })
    @PostMapping("projects/{projectKey}/workflows")
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

    @Operation(summary = "Replace workflow graph", description = """
                Replace the entire workflow graph (states and transitions) in a single operation.
                 Existing nodes use `id`, new nodes must use a client-generated `tempKey`
                 that is unique within the request. Nodes not included are deleted.

                When deleted states have active issues, `stateMigrationRequests` must map each
                 deleted state to a target state.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workflow graph replaced"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request or invalid graph structure",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workflow not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Version conflict or name conflict", content = @Content)
    })
    @PatchMapping("workflows/{workflowId}/graph")
    public ResponseEntity<Void> replaceWorkflowGraph(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @RequestBody @Valid ReplaceWorkflowGraphRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        workflowGraphReplaceUseCase.replaceWorkflowGraph(
                workspaceKey, workflowId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update workflow", description = """
                Update a workflow's name, description, or color.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workflow updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workflow not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Workflow name already exists", content = @Content)
    })
    @PatchMapping("workflows/{workflowId}")
    public ResponseEntity<Void> updateWorkflow(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @RequestBody @Valid UpdateWorkflowRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        workflowCommandUseCase.update(workspaceKey, workflowId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update VCS settings", description = """
                Configure the VCS integration settings for a workflow.
                 Maps VCS events (PR opened, PR merged) to workflow transitions.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "VCS settings updated"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workflow or transition not found", content = @Content)
    })
    @PatchMapping("workflows/{workflowId}/vcs-settings")
    public ResponseEntity<Void> updateWorkflowVcsSettings(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @RequestBody @Valid UpdateWorkflowVcsSettingsRequest request,
            @CurrentMember MemberDetails memberDetails) {
        workflowCommandUseCase.updateVcsSettings(
                workspaceKey, workflowId, request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete workflow", description = """
                Delete a workflow from the project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workflow deleted"),
        @ApiResponse(responseCode = "400", description = "Workflow has active issues", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workflow not found", content = @Content)
    })
    @DeleteMapping("workflows/{workflowId}")
    public ResponseEntity<Void> archiveWorkflow(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @CurrentMember MemberDetails memberDetails) {
        workflowCommandUseCase.delete(workspaceKey, workflowId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update state", description = """
                Update a workflow state's name, description, or color.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "State updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "State not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "State name already exists", content = @Content)
    })
    @PatchMapping("workflows/{workflowId}/states/{stateId}")
    public ResponseEntity<Void> updateWorkflowState(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @PathVariable Long stateId,
            @RequestBody @Valid UpdateStateRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        workflowCommandUseCase.updateState(workspaceKey, workflowId, stateId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update transition", description = """
                Update a workflow transition's name or description.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Transition updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Transition not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Transition name already exists", content = @Content)
    })
    @PatchMapping("workflows/{workflowId}/transitions/{transitionId}")
    public ResponseEntity<Void> updateWorkflowTransition(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @PathVariable Long transitionId,
            @RequestBody @Valid UpdateTransitionRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        workflowCommandUseCase.updateTransition(
                workspaceKey, workflowId, transitionId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Configure transition guards", description = """
                Set the guard conditions for a workflow transition.
                 Replaces all existing guards with the provided list.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Guards configured"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request or invalid guard parameters",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workflow or transition not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Duplicate guard type", content = @Content)
    })
    @PutMapping("workflows/{workflowId}/transitions/{transitionId}/guards")
    public ResponseEntity<Void> configureTransitionGuards(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @PathVariable Long transitionId,
            @RequestBody @Valid ConfigureTransitionGuardsRequest request,
            @CurrentMember MemberDetails memberDetails) {
        workflowCommandUseCase.configureTransitionGuards(
                workspaceKey, workflowId, transitionId, request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "List workflows", description = "Retrieve all workflows in the project.")
    @ApiResponse(responseCode = "200", description = "Workflows retrieved")
    @GetMapping("projects/{projectKey}/workflows")
    public ResponseEntity<List<WorkflowSummary>> getWorkflows(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        List<WorkflowSummary> workflows = workflowQueryUseCase.getWorkflows(
                ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.ok(workflows);
    }

    @Operation(
            summary = "Get workflow detail",
            description = "Retrieve the full detail of a workflow including states, transitions, and guards.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workflow detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Workflow not found", content = @Content)
    })
    @GetMapping("workflows/{workflowId}")
    public ResponseEntity<WorkflowDetail> getWorkflowDetail(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @CurrentMember MemberDetails memberDetails) {
        WorkflowDetail detail =
                workflowQueryUseCase.getWorkflowDetail(workspaceKey, workflowId, memberDetails.getMemberId());

        return ResponseEntity.ok(detail);
    }

    @Operation(summary = "Check state name availability", description = """
                Check whether a state name is available within the workflow.

                **Requirements:**
                - `name` must be unique within the workflow""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "State name is available"),
        @ApiResponse(responseCode = "409", description = "State name already exists", content = @Content)
    })
    @GetMapping("workflows/{workflowId}/check-state-name")
    public ResponseEntity<Void> checkStateNameAvailability(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @RequestParam String name,
            @CurrentMember MemberDetails memberDetails) {
        workflowQueryUseCase.checkStateNameUniqueness(workspaceKey, workflowId, name, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
