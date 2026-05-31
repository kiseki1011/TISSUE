package com.tissue.feature.workflow.web;

import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowStateCounts;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import com.tissue.feature.workflow.application.port.usecase.WorkflowQueryUseCase;
import com.tissue.feature.workflow.domain.exception.WorkflowErrorCode;
import com.tissue.global.openapi.WorkflowErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
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

@Tag(name = "Workflow")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkflowQueryController {

    private final WorkflowQueryUseCase workflowQueryUseCase;

    @Operation(operationId = "listWorkflows", summary = "List workflows", description = """
                    List all workflows.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workflows retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @GetMapping("workflows")
    public ResponseEntity<List<WorkflowSummary>> listWorkflows(@CurrentMember MemberDetails memberDetails) {
        List<WorkflowSummary> workflows = workflowQueryUseCase.getWorkflows(memberDetails.getMemberId());

        return ResponseEntity.ok(workflows);
    }

    @Operation(operationId = "getWorkflow", summary = "Get workflow detail", description = """
                Get a workflow with its states, transitions, and guards.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workflow detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkflowErrors({WorkflowErrorCode.WORKFLOW_NOT_FOUND})
    @GetMapping("workflows/{workflowId}")
    public ResponseEntity<WorkflowDetail> getWorkflow(
            @PathVariable Long workflowId, @CurrentMember MemberDetails memberDetails) {
        WorkflowDetail detail = workflowQueryUseCase.getWorkflowDetail(workflowId, memberDetails.getMemberId());

        return ResponseEntity.ok(detail);
    }

    @Operation(operationId = "getWorkflowStateCounts", summary = "Get state issue counts", description = """
                Get active (non soft-deleted) issue count per state of the workflow.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Counts retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkflowErrors({WorkflowErrorCode.WORKFLOW_NOT_FOUND})
    @GetMapping("workflows/{workflowId}/state-counts")
    public ResponseEntity<WorkflowStateCounts> getWorkflowStateCounts(
            @PathVariable Long workflowId, @CurrentMember MemberDetails memberDetails) {
        WorkflowStateCounts counts =
                workflowQueryUseCase.getWorkflowStateCounts(workflowId, memberDetails.getMemberId());

        return ResponseEntity.ok(counts);
    }

    @Operation(
            operationId = "checkWorkflowStateNameAvailability",
            summary = "Check state name availability",
            description = """
                    Check whether a state name is available (unique) within the workflow.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "State name is available"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkflowErrors({WorkflowErrorCode.WORKFLOW_NOT_FOUND, WorkflowErrorCode.DUPLICATE_STATE_NAME})
    @GetMapping("workflows/{workflowId}:checkStateName")
    public ResponseEntity<Void> checkWorkflowStateNameAvailability(
            @PathVariable Long workflowId,
            @Parameter(description = "State name to check") @RequestParam String name,
            @CurrentMember MemberDetails memberDetails) {
        workflowQueryUseCase.checkStateNameUniqueness(workflowId, name, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
