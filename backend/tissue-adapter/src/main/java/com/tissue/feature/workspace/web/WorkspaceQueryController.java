package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.query.DeletedWorkspaceSummary;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceQueryUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
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

@Tag(name = "Workspace")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceQueryController {

    private final WorkspaceQueryUseCase workspaceQueryUseCase;

    @Operation(operationId = "getWorkspace", summary = "Get workspace detail", description = """
                    Get a workspace's full detail including its settings.

                    **Requirements:**
                    - Requires workspace membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workspace detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @GetMapping("/{workspaceKey}")
    public ResponseEntity<WorkspaceDetail> getWorkspace(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        WorkspaceDetail response = workspaceQueryUseCase.getDetail(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "listMyWorkspaces", summary = "List my workspaces", description = """
                    List all workspaces the current member belongs to.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Workspace list retrieved")})
    @GetMapping("/me")
    public ResponseEntity<List<WorkspaceSummaryResponse>> listMyWorkspaces(@CurrentMember MemberDetails memberDetails) {
        List<WorkspaceSummaryResponse> response = workspaceQueryUseCase.getMyWorkspaces(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "listMyDeletedWorkspaces", summary = "List deleted workspaces", description = """
                    List all soft-deleted workspaces owned by the current member that are still within \
                    the retention period.

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Deleted workspace list retrieved")})
    @GetMapping("/deleted")
    public ResponseEntity<List<DeletedWorkspaceSummary>> listMyDeletedWorkspaces(
            @CurrentMember MemberDetails memberDetails) {
        List<DeletedWorkspaceSummary> response =
                workspaceQueryUseCase.getMyDeletedWorkspaces(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "checkWorkspaceKeyAvailability",
            summary = "Check workspace key availability",
            description = """
                    Check whether a workspace key is available. Case-insensitive (keys are stored uppercase).

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace key is available"),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.DUPLICATE_WORKSPACE_KEY})
    @GetMapping(":checkKey")
    public ResponseEntity<Void> checkWorkspaceKeyAvailability(
            @Parameter(description = "Workspace key to check") @RequestParam String key) {
        workspaceQueryUseCase.checkKeyAvailability(key);

        return ResponseEntity.noContent().build();
    }
}
