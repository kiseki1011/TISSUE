package com.tissue.feature.project.application.port.usecase;

import com.tissue.feature.project.application.dto.response.ProjectAgingStats;
import com.tissue.feature.project.application.dto.response.ProjectContributionStats;
import com.tissue.feature.project.application.dto.response.ProjectCycleTimeStats;
import com.tissue.feature.project.application.dto.response.ProjectFlowStats;
import com.tissue.feature.project.application.dto.response.ProjectMemberStats;
import com.tissue.feature.project.application.dto.response.ProjectSimpleStats;
import com.tissue.feature.project.application.dto.response.ProjectSprintReport;
import com.tissue.feature.project.application.dto.response.ProjectVelocity;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Read-only project statistics, split from {@link ProjectQueryUseCase} because statistics change
 * technology independently of plain project queries and carry their own authorization policy.
 *
 * <p>Authorization has two tiers. {@link #getProjectSimpleStats} is a browse-time preview: a PUBLIC
 * project's coarse counts are visible to any active member (project discovery), a PRIVATE project's only
 * to its members. Every other method exposes per-member and time-series internals and is limited to
 * project members, matching how the underlying issue data is gated.
 */
public interface ProjectStatsQueryUseCase {

    ProjectSimpleStats getProjectSimpleStats(ProjectIdentifier pid, Long actorMemberId);

    List<ProjectMemberStats> getProjectMemberStats(ProjectIdentifier pid, Long actorMemberId);

    ProjectAgingStats getProjectAgingStats(ProjectIdentifier pid, Long actorMemberId);

    ProjectFlowStats getProjectFlowStats(
            ProjectIdentifier pid, String window, @Nullable Long sprintId, @Nullable String zoneId, Long actorMemberId);

    ProjectCycleTimeStats getProjectCycleTimeStats(
            ProjectIdentifier pid, String window, @Nullable Long sprintId, Long actorMemberId);

    ProjectSprintReport getProjectSprintReport(ProjectIdentifier pid, Long sprintId, Long actorMemberId);

    ProjectVelocity getProjectVelocity(ProjectIdentifier pid, Long actorMemberId);

    ProjectContributionStats getProjectContributions(
            ProjectIdentifier pid, Long memberId, int days, @Nullable String zoneId, Long actorMemberId);
}
