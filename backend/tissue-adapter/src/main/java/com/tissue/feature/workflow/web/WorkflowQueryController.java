package com.tissue.feature.workflow.web;

import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import com.tissue.feature.workflow.application.port.usecase.WorkflowQueryUseCase;
import com.tissue.feature.workflow.domain.exception.WorkflowErrorCode;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.WorkflowErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workflow Query")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class WorkflowQueryController {

    private final WorkflowQueryUseCase workflowQueryUseCase;

    @Operation(operationId = "listWorkflows", summary = "List workflows", description = """
                    List workflows of a project.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workflows retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND, ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping("projects/{projectKey}/workflows")
    public ResponseEntity<List<WorkflowSummary>> listWorkflows(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        List<WorkflowSummary> workflows = workflowQueryUseCase.getWorkflows(
                ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.ok(workflows);
    }

    @Operation(operationId = "getWorkflow", summary = "Get workflow detail", description = """
                Get a workflow with its states, transitions, and guards.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workflow detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @WorkflowErrors({WorkflowErrorCode.WORKFLOW_NOT_FOUND})
    @GetMapping("workflows/{workflowId}")
    public ResponseEntity<WorkflowDetail> getWorkflow(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @CurrentMember MemberDetails memberDetails) {
        WorkflowDetail detail =
                workflowQueryUseCase.getWorkflowDetail(workspaceKey, workflowId, memberDetails.getMemberId());

        return ResponseEntity.ok(detail);
    }

    @Operation(
            operationId = "checkWorkflowStateNameAvailability",
            summary = "Check state name availability",
            description = """
                    Check whether a state name is available (unique) within the workflow.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "State name is available"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @WorkflowErrors({WorkflowErrorCode.WORKFLOW_NOT_FOUND, WorkflowErrorCode.DUPLICATE_STATE_NAME})
    @GetMapping("workflows/{workflowId}:checkStateName")
    public ResponseEntity<Void> checkWorkflowStateNameAvailability(
            @PathVariable String workspaceKey,
            @PathVariable Long workflowId,
            @Parameter(description = "State name to check") @RequestParam String name,
            @CurrentMember MemberDetails memberDetails) {
        workflowQueryUseCase.checkStateNameUniqueness(workspaceKey, workflowId, name, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
