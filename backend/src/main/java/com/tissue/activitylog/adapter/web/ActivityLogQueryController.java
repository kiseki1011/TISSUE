package com.tissue.activitylog.adapter.web;

import com.tissue.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.activitylog.application.service.ActivityLogQueryService;
import com.tissue.common.dto.CursorPageResponse;
import com.tissue.project.adapter.web.resolver.CurrentProjectMember;
import com.tissue.project.application.dto.ProjectMemberContext;
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

    private final ActivityLogQueryService queryService;

    @GetMapping("/issues/{issueKey}/activities")
    public ResponseEntity<CursorPageResponse<ActivityLogResponse>> getIssueActivities(
        @PathVariable String workspaceKey,
        @PathVariable String projectKey,
        @PathVariable String issueKey,
        @RequestParam(required = false) Long lastLogId,
        @RequestParam(defaultValue = "20") int limit,
        @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        CursorPageResponse<ActivityLogResponse> response =
            queryService.getIssueActivities(currentProjectMember, issueKey, lastLogId, limit);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/sprints/{sprintId}/activities")
    public ResponseEntity<CursorPageResponse<ActivityLogResponse>> getSprintActivities(
        @PathVariable String workspaceKey,
        @PathVariable String projectKey,
        @PathVariable Long sprintId,
        @RequestParam(required = false) Long lastLogId,
        @RequestParam(defaultValue = "20") int limit,
        @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        CursorPageResponse<ActivityLogResponse> response =
            queryService.getSprintActivities(currentProjectMember, sprintId, lastLogId, limit);

        return ResponseEntity.ok(response);
    }
}
