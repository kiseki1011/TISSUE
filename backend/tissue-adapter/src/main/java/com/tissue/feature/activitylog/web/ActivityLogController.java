package com.tissue.feature.activitylog.web;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.service.ActivityLogQueryService;
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
public class ActivityLogController {

    private final ActivityLogQueryService activityLogQueryService;

    @Operation(
            operationId = "listIssueActivities",
            summary = "Get issue activity log",
            description = "Retrieve activity logs for an issue with keyset-based pagination.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Activity logs retrieved"),
        @ApiResponse(responseCode = "404", description = "Issue not found", content = @Content)
    })
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
        KeysetPageResponse<ActivityLogResponse> response = activityLogQueryService.getIssueActivities(
                IssueIdentifier.of(workspaceKey, issueKey), memberDetails.getMemberId(), keysetId, limit);

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "listSprintActivities",
            summary = "Get sprint activity log",
            description = "Retrieve activity logs for a sprint with keyset-based pagination.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Activity logs retrieved"),
        @ApiResponse(responseCode = "404", description = "Sprint not found", content = @Content)
    })
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
        KeysetPageResponse<ActivityLogResponse> response = activityLogQueryService.getSprintActivities(
                workspaceKey, sprintId, memberDetails.getMemberId(), keysetId, limit);

        return ResponseEntity.ok(response);
    }
}
