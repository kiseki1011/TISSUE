package com.tissue.admin.adapter.web;

import com.tissue.admin.application.port.usecase.AdminActivityLogUseCase;
import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.auth.RequireSuperAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Global Activity Log")
@RestController
@RequestMapping("/api/v1/admin/activity-logs")
@RequiredArgsConstructor
@RequireSuperAdmin
public class AdminActivityLogController {

    private final AdminActivityLogUseCase adminActivityLogUseCase;

    @Operation(
            operationId = "adminListActivities",
            summary = "List activity logs across all projects",
            description = """
                Cross-project view of the product activity log (issue/sprint events). Unlike the
                per-issue/per-sprint endpoints, this is not limited to projects the caller belongs to.
                Optional `projectKey`, `issueKey`, `actorMemberId`, and `activityType` filters.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Activity logs retrieved")})
    @GetMapping
    public ResponseEntity<Page<ActivityLogResponse>> listActivities(
            @RequestParam(required = false) @Nullable String projectKey,
            @RequestParam(required = false) @Nullable String issueKey,
            @RequestParam(required = false) @Nullable Long actorMemberId,
            @RequestParam(required = false) @Nullable ActivityType activityType,
            Pageable pageable) {
        Page<ActivityLogResponse> response =
                adminActivityLogUseCase.listActivities(projectKey, issueKey, actorMemberId, activityType, pageable);
        return ResponseEntity.ok(response);
    }
}
