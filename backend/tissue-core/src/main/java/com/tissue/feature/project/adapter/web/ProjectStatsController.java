package com.tissue.feature.project.adapter.web;

import com.tissue.feature.project.application.dto.response.ProjectAgingStats;
import com.tissue.feature.project.application.dto.response.ProjectContributionStats;
import com.tissue.feature.project.application.dto.response.ProjectCycleTimeStats;
import com.tissue.feature.project.application.dto.response.ProjectFlowStats;
import com.tissue.feature.project.application.dto.response.ProjectMemberStats;
import com.tissue.feature.project.application.dto.response.ProjectSimpleStats;
import com.tissue.feature.project.application.dto.response.ProjectSprintReport;
import com.tissue.feature.project.application.dto.response.ProjectVelocity;
import com.tissue.feature.project.application.port.usecase.ProjectStatsQueryUseCase;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import io.swagger.v3.oas.annotations.Operation;
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

/**
 * Project statistics endpoints. Split from {@code ProjectQueryController} so statistics carry their own
 * authorization: simple-stats is a PUBLIC-visible browse preview, every other endpoint is member-only.
 */
@LLMGenerated(llmInvolvement = LLMInvolvement.ASSISTED, model = "claude-opus-4-8")
@Tag(name = "Project")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectStatsController {

    private final ProjectStatsQueryUseCase projectStatsQueryUseCase;

    @Operation(operationId = "getProjectSimpleStats", summary = "Get project simple statistics", description = """
                Snapshot statistics for a project's current issues: totals, open and completed counts, \
                unassigned and overdue KPIs, and count breakdowns by state category, hierarchy and priority. \
                Stats are count-based. Soft-deleted issues are excluded. A PUBLIC project's stats are visible \
                to any member (project discovery); a PRIVATE project's only to its members.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Project statistics retrieved"),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @GetMapping("/{projectKey}/simple-stats")
    public ResponseEntity<ProjectSimpleStats> getProjectSimpleStats(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        ProjectSimpleStats response = projectStatsQueryUseCase.getProjectSimpleStats(
                ProjectIdentifier.ofProjectKey(projectKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getProjectMemberStats", summary = "Get per-member contribution stats", description = """
                Per-member contribution stats for a project: resolved and open assigned counts, resolved \
                story points, and completion rate. A row is returned only for members that have at least \
                one assigned issue; soft-deleted issues are excluded. Limited to project members.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Member stats retrieved"),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @GetMapping("/{projectKey}/member-stats")
    public ResponseEntity<List<ProjectMemberStats>> getProjectMemberStats(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        List<ProjectMemberStats> response = projectStatsQueryUseCase.getProjectMemberStats(
                ProjectIdentifier.ofProjectKey(projectKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getProjectAgingStats", summary = "Get project aging & blocked stats", description = """
                Aging and flow-risk snapshot for a project's open issues: how long open work has been \
                sitting, bucketed by age (measured from when an issue started, or its creation when not yet \
                started), plus how many open issues are currently blocked by a still-open blocker. Always \
                computed as of now; soft-deleted issues are excluded. Limited to project members.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Aging stats retrieved"),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @GetMapping("/{projectKey}/stats/aging")
    public ResponseEntity<ProjectAgingStats> getProjectAgingStats(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        ProjectAgingStats response = projectStatsQueryUseCase.getProjectAgingStats(
                ProjectIdentifier.ofProjectKey(projectKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getProjectFlowStats", summary = "Get project flow statistics", description = """
                Flow statistics over a time window. A dense, one-per-day series of how many issues were \
                created and how many were resolved (entered a COMPLETED state). The `window` selects a \
                preset - `week` (rolling 7 days), `month` (rolling 30 days) or `sprint` (the sprint's \
                active period, requires `sprintId`). An unknown value falls back to `month`. The `zoneId` selects the IANA zone the days are cut on (e.g. `Asia/Seoul`); \
                it defaults to UTC, and an unrecognized zone falls back to UTC rather than failing. \
                Soft-deleted \
                issues are excluded. Reopened issues drop out of the resolved series until resolved again. \
                Limited to project members.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Flow stats retrieved"),
        @ApiResponse(responseCode = "404", description = "Project or sprint not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @GetMapping("/{projectKey}/stats/flow")
    public ResponseEntity<ProjectFlowStats> getProjectFlowStats(
            @PathVariable String projectKey,
            @RequestParam(value = "window", defaultValue = "month") String window,
            @RequestParam(value = "sprintId", required = false) Long sprintId,
            @RequestParam(value = "zoneId", required = false) String zoneId,
            @CurrentMember MemberDetails memberDetails) {
        ProjectFlowStats response = projectStatsQueryUseCase.getProjectFlowStats(
                ProjectIdentifier.ofProjectKey(projectKey), window, sprintId, zoneId, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "getProjectCycleTimeStats",
            summary = "Get project cycle & lead time stats",
            description = """
                Cycle- and lead-time statistics over a time window, computed from issues currently in a \
                COMPLETED state and resolved within the window. Cycle time measures start-of-work to \
                resolution (issues that never started are excluded); lead time measures creation to \
                resolution. Each reports count, average, p50 and p90 in seconds. The `window` selects a \
                preset - `week`, `month` or `sprint` (requires `sprintId`); an unknown value falls back to \
                `month`. Soft-deleted issues are excluded. Limited to project members.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cycle time stats retrieved"),
        @ApiResponse(responseCode = "404", description = "Project or sprint not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @GetMapping("/{projectKey}/stats/cycle-time")
    public ResponseEntity<ProjectCycleTimeStats> getProjectCycleTimeStats(
            @PathVariable String projectKey,
            @RequestParam(value = "window", defaultValue = "month") String window,
            @RequestParam(value = "sprintId", required = false) Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        ProjectCycleTimeStats response = projectStatsQueryUseCase.getProjectCycleTimeStats(
                ProjectIdentifier.ofProjectKey(projectKey), window, sprintId, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getProjectSprintReport", summary = "Get a sprint report", description = """
                A snapshot report for one sprint: total, completed and still-open (carried) issue counts, \
                completion rate, story points (excluding EPICs) and the issue distribution by state \
                category. Reflects the sprint's current scope - there is no scope history, so this is a \
                live snapshot (the final tally once the sprint is completed), not a committed-vs-delivered \
                burndown. Requires `sprintId`. Soft-deleted issues are excluded. Limited to project members.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sprint report retrieved"),
        @ApiResponse(responseCode = "404", description = "Project or sprint not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @GetMapping("/{projectKey}/stats/sprint-report")
    public ResponseEntity<ProjectSprintReport> getProjectSprintReport(
            @PathVariable String projectKey,
            @RequestParam(value = "sprintId") Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        ProjectSprintReport response = projectStatsQueryUseCase.getProjectSprintReport(
                ProjectIdentifier.ofProjectKey(projectKey), sprintId, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getProjectVelocity", summary = "Get project sprint velocity", description = """
                Delivered work per COMPLETED sprint, oldest first: completed story points (excluding EPICs) \
                and completed issue count for each sprint, plus the mean over those sprints. Reflects each \
                issue's current sprint membership and state (there is no scope history), so a sprint whose \
                scope changed after it closed reports its live tally. Soft-deleted issues are excluded. \
                Limited to project members.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Velocity retrieved"),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @GetMapping("/{projectKey}/stats/velocity")
    public ResponseEntity<ProjectVelocity> getProjectVelocity(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        ProjectVelocity response = projectStatsQueryUseCase.getProjectVelocity(
                ProjectIdentifier.ofProjectKey(projectKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "getProjectContributions",
            summary = "Get a member's contribution heatmap",
            description = """
                A per-member issue-resolution heatmap ("잔디"): a dense one-per-day count of issues the \
                member resolved over the last `days` days (default 90, capped at 366), zero-filled. \
                The `zoneId` selects the IANA zone the days are cut on (e.g. `Asia/Seoul`); \
                it defaults to UTC, and an unrecognized zone falls back to UTC rather than failing. \
                Resolution is attributed by an issue's current assignee, counting issues \
                currently in a COMPLETED state by their resolution day. Soft-deleted issues are excluded. \
                Limited to project members.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Contribution heatmap retrieved"),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @GetMapping("/{projectKey}/stats/contributions")
    public ResponseEntity<ProjectContributionStats> getProjectContributions(
            @PathVariable String projectKey,
            @RequestParam(value = "memberId") Long memberId,
            @RequestParam(value = "days", defaultValue = "90") int days,
            @RequestParam(value = "zoneId", required = false) String zoneId,
            @CurrentMember MemberDetails memberDetails) {
        ProjectContributionStats response = projectStatsQueryUseCase.getProjectContributions(
                ProjectIdentifier.ofProjectKey(projectKey), memberId, days, zoneId, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
