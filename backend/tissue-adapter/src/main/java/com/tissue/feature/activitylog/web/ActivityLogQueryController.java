package com.tissue.feature.activitylog.web;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.service.ActivityLogQueryService;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.shared.dto.CursorPageResponse;
import com.tissue.shared.dto.IssueIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}")
@RequiredArgsConstructor
public class ActivityLogQueryController {

    private final ActivityLogQueryService activityLogQueryService;

    @GetMapping("/issues/{issueKey}/activities")
    public ResponseEntity<CursorPageResponse<ActivityLogResponse>> getIssueActivities(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable String issueKey,
            @RequestParam(required = false) Long lastLogId,
            @RequestParam(defaultValue = "20") int limit,
            @CurrentMember MemberDetails memberDetails) {
        CursorPageResponse<ActivityLogResponse> response = activityLogQueryService.getIssueActivities(
                IssueIdentifier.of(workspaceKey, projectKey, issueKey), memberDetails.getMemberId(), lastLogId, limit);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sprints/{sprintId}/activities")
    public ResponseEntity<CursorPageResponse<ActivityLogResponse>> getSprintActivities(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @RequestParam(required = false) Long lastLogId,
            @RequestParam(defaultValue = "20") int limit,
            @CurrentMember MemberDetails memberDetails) {
        CursorPageResponse<ActivityLogResponse> response = activityLogQueryService.getSprintActivities(
                workspaceKey, sprintId, memberDetails.getMemberId(), lastLogId, limit);

        return ResponseEntity.ok(response);
    }
}
