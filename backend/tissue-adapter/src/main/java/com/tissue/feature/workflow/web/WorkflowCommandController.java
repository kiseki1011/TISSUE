package com.tissue.feature.workflow.web;

import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.feature.workflow.application.port.usecase.WorkflowCommandUseCase;
import com.tissue.feature.workflow.application.port.usecase.WorkflowGraphReplaceUseCase;
import com.tissue.feature.workflow.domain.exception.WorkflowErrorCode;
import com.tissue.feature.workflow.web.request.ConfigureTransitionGuardsRequest;
import com.tissue.feature.workflow.web.request.CreateWorkflowRequest;
import com.tissue.feature.workflow.web.request.ReplaceWorkflowGraphRequest;
import com.tissue.feature.workflow.web.request.UpdateStateRequest;
import com.tissue.feature.workflow.web.request.UpdateTransitionRequest;
import com.tissue.feature.workflow.web.request.UpdateWorkflowRequest;
import com.tissue.feature.workflow.web.request.UpdateWorkflowVcsSettingsRequest;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.WorkflowErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@Tag(name = "Workflow")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class WorkflowCommandController {

    private final WorkflowCommandUseCase workflowCommandUseCase;
    private final WorkflowGraphReplaceUseCase workflowGraphReplaceUseCase;

    @Operation(operationId = "createWorkflow", summary = "Create workflow", description = """
                Create a new workflow with states and transitions.
                Each state must include a client-generated `tempKey`
                that is unique within the request.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Workflow created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @WorkflowErrors({
        WorkflowErrorCode.TEMP_KEY_NOT_RESOLVED,
        WorkflowErrorCode.DUPLICATE_STATE_NAME,
        WorkflowErrorCode.DUPLICATE_TRANSITION_NAME,
        WorkflowErrorCode.DUPLICATE_TRANSITION_EDGE,
        WorkflowErrorCode.DUPLICATE_WORKFLOW_NAME,
        WorkflowErrorCode.INVALID_INITIAL_STATE_COUNT,
        WorkflowErrorCode.INVALID_TRANSITION_TARGET,
        WorkflowErrorCode.MISSING_COMPLETED_STATE,
        WorkflowErrorCode.ORPHAN_STATE,
        WorkflowErrorCode.DEAD_END_STATE,
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

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "replaceWorkflowGraph", summary = "Replace workflow graph", description = """
                Replace the entire workflow graph (states and transitions) in a single operation.
                Existing nodes use `id`, new nodes must use a client-generated `tempKey`
                that is unique within the request. Nodes not included are deleted.

                When deleted states have active issues, `stateMigrationRequests` must map each
                 deleted state to a target state.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workflow graph replaced"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @WorkflowErrors({
        WorkflowErrorCode.WORKFLOW_NOT_FOUND,
        WorkflowErrorCode.WORKFLOW_TRANSITION_NOT_FOUND,
        WorkflowErrorCode.WORKFLOW_VERSION_MISMATCH,
        WorkflowErrorCode.INCOMPLETE_NEW_STATE,
        WorkflowErrorCode.INCOMPLETE_NEW_TRANSITION,
        WorkflowErrorCode.INVALID_INITIAL_STATE_COUNT,
        WorkflowErrorCode.INVALID_TRANSITION_TARGET,
        WorkflowErrorCode.MISSING_COMPLETED_STATE,
        WorkflowErrorCode.ORPHAN_STATE,
        WorkflowErrorCode.DEAD_END_STATE,
        WorkflowErrorCode.DUPLICATE_STATE_NAME,
        WorkflowErrorCode.DUPLICATE_TRANSITION_NAME,
        WorkflowErrorCode.DUPLICATE_TRANSITION_EDGE,
        WorkflowErrorCode.INITIAL_STATE_BELONG_MISMATCH,
        WorkflowErrorCode.INITIAL_STATE_CATEGORY_MISMATCH,
        WorkflowErrorCode.MIGRATION_TARGET_BEING_DELETED,
        WorkflowErrorCode.STATE_MIGRATION_REQUIRED,
        WorkflowErrorCode.WORKFLOW_STATE_IN_USE,
    })
    @PutMapping("projects/{projectKey}/workflows/{workflowId}/graph")
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

    @Operation(operationId = "updateWorkflow", summary = "Update workflow", description = """
                Update a workflow's name, description, or color. Only provided fields are updated.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workflow updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @WorkflowErrors({
        WorkflowErrorCode.WORKFLOW_NOT_FOUND,
        WorkflowErrorCode.DUPLICATE_WORKFLOW_NAME,
    })
    @PatchMapping("projects/{projectKey}/workflows/{workflowId}")
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

    @Operation(operationId = "updateWorkflowVcsSettings", summary = "Update VCS settings", description = """
                Configure the VCS integration settings for a workflow.
                Maps VCS events (PR opened, PR merged) to workflow transitions.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "VCS settings updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @WorkflowErrors({
        WorkflowErrorCode.WORKFLOW_NOT_FOUND,
        WorkflowErrorCode.WORKFLOW_TRANSITION_NOT_FOUND,
    })
    @PatchMapping("projects/{projectKey}/workflows/{workflowId}/vcs-settings")
    public ResponseEntity<Void> updateWorkflowVcsSettings(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long workflowId,
            @RequestBody @Valid UpdateWorkflowVcsSettingsRequest request,
            @CurrentMember MemberDetails memberDetails) {
        workflowCommandUseCase.updateVcsSettings(
                ProjectIdentifier.of(workspaceKey, projectKey),
                workflowId,
                request.toCommand(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteWorkflow", summary = "Delete workflow", description = """
                Permanently deletes a workflow from the project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workflow deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @WorkflowErrors({
        WorkflowErrorCode.WORKFLOW_NOT_FOUND,
        WorkflowErrorCode.WORKFLOW_STATE_IN_USE,
    })
    @DeleteMapping("projects/{projectKey}/workflows/{workflowId}")
    public ResponseEntity<Void> deleteWorkflow(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long workflowId,
            @CurrentMember MemberDetails memberDetails) {
        workflowCommandUseCase.delete(
                ProjectIdentifier.of(workspaceKey, projectKey), workflowId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "updateWorkflowState", summary = "Update state", description = """
                Update a workflow state's name, description, or color. Only provided fields are updated.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "State updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @WorkflowErrors({
        WorkflowErrorCode.WORKFLOW_STATE_NOT_FOUND,
        WorkflowErrorCode.DUPLICATE_STATE_NAME,
    })
    @PatchMapping("projects/{projectKey}/workflows/{workflowId}/states/{stateId}")
    public ResponseEntity<Void> updateWorkflowState(
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

    @Operation(operationId = "updateWorkflowTransition", summary = "Update transition", description = """
                Update a workflow transition's name or description. Only provided fields are updated.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Transition updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @WorkflowErrors({
        WorkflowErrorCode.WORKFLOW_TRANSITION_NOT_FOUND,
        WorkflowErrorCode.DUPLICATE_TRANSITION_NAME,
    })
    @PatchMapping("projects/{projectKey}/workflows/{workflowId}/transitions/{transitionId}")
    public ResponseEntity<Void> updateWorkflowTransition(
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

    @Operation(operationId = "configureTransitionGuards", summary = "Configure transition guards", description = """
                Set the guard conditions for a workflow transition.
                 Replaces all existing guards with the provided list.

                **Available guard types:**
                - `NOT_BLOCKED` — Ensures the issue is not blocked by other issues. No params required.
                - `ASSIGNEE_REQUIRED` — Requires at least one assignee on the issue. No params required.
                - `CHILD_ISSUES_RESOLVED` — All child issues must be resolved. No params required.
                - `REQUIRED_APPROVAL` — Requires reviewer approvals before transition.

                **`REQUIRED_APPROVAL` guard parameters:**
                - `min_approvals` (number, default: 1) — Minimum number of `APPROVED` reviewers \
                required to pass the guarded transition.
                - `block_on_change_request` (boolean, default: true) — If any reviewer has \
                `CHANGES_REQUESTED` status, the guarded transition is blocked.
                - `auto_transition_on_reject` (boolean, default: false) — Enables automatic \
                state transition when a reviewer rejects.
                - `reject_transition_name` (text, required if auto-reject enabled) — \
                The name of the transition to execute automatically on rejection.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Guards configured"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content),
        @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @WorkflowErrors({
        WorkflowErrorCode.WORKFLOW_NOT_FOUND,
        WorkflowErrorCode.WORKFLOW_TRANSITION_NOT_FOUND,
        WorkflowErrorCode.DUPLICATE_GUARD_TYPE,
        WorkflowErrorCode.INVALID_GUARD_PARAMETER,
        WorkflowErrorCode.GUARD_NOT_FOUND,
    })
    @PutMapping("projects/{projectKey}/workflows/{workflowId}/transitions/{transitionId}/guards")
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
}
