package com.tissue.feature.activitylog.web;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.usecase.ActivityLogQueryUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.KeysetPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Activity Log")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class ActivityLogQueryController {

    private final ActivityLogQueryUseCase activityLogQueryUseCase;

    @Operation(operationId = "listIssueActivities", summary = "List issue activities", description = """
                    List activity logs of an issue. Uses keyset pagination ordered by id descending.

                    **Requirements:**
                    - Requires workspace membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Activity logs retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @GetMapping("issues/{issueKey}/activities")
    public ResponseEntity<KeysetPageResponse<ActivityLogResponse>> listIssueActivities(
            @PathVariable String workspaceKey,
            @PathVariable String issueKey,
            @Parameter(description = "ID of the last item from the previous page. Leave empty for the first page.")
                    @RequestParam(required = false)
                    Long keysetId,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20")
                    int limit,
            @CurrentMember MemberDetails memberDetails) {
        KeysetPageResponse<ActivityLogResponse> response = activityLogQueryUseCase.getIssueActivities(
                IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId(), keysetId, limit);

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "listSprintActivities", summary = "List sprint activities", description = """
                    List activity logs of a sprint. Uses keyset pagination ordered by id descending.

                    **Requirements:**
                    - Requires workspace membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Activity logs retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @GetMapping("sprints/{sprintId}/activities")
    public ResponseEntity<KeysetPageResponse<ActivityLogResponse>> listSprintActivities(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @Parameter(description = "ID of the last item from the previous page. Leave empty for the first page.")
                    @RequestParam(required = false)
                    Long keysetId,
            @Parameter(description = "Number of items per page", example = "20") @RequestParam(defaultValue = "20")
                    int limit,
            @CurrentMember MemberDetails memberDetails) {
        KeysetPageResponse<ActivityLogResponse> response = activityLogQueryUseCase.getSprintActivities(
                workspaceKey, sprintId, memberDetails.getMemberId(), keysetId, limit);

        return ResponseEntity.ok(response);
    }
}
