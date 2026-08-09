package com.tissue.feature.project.application.service;

import com.tissue.feature.issue.application.port.repository.HierarchyCountRow;
import com.tissue.feature.issue.application.port.repository.IssueStatsQueryRepository;
import com.tissue.feature.issue.application.port.repository.PriorityCountRow;
import com.tissue.feature.issue.application.port.repository.ProjectAgingRow;
import com.tissue.feature.issue.application.port.repository.ProjectStatsKpiRow;
import com.tissue.feature.issue.application.port.repository.StateCategoryCountRow;
import com.tissue.feature.issue.application.port.repository.TimestampPairRow;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.project.application.dto.StatsWindow;
import com.tissue.feature.project.application.dto.request.StatsWindowType;
import com.tissue.feature.project.application.dto.response.ProjectAgingStats;
import com.tissue.feature.project.application.dto.response.ProjectContributionStats;
import com.tissue.feature.project.application.dto.response.ProjectContributionStats.ContributionDay;
import com.tissue.feature.project.application.dto.response.ProjectCycleTimeStats;
import com.tissue.feature.project.application.dto.response.ProjectCycleTimeStats.DurationStats;
import com.tissue.feature.project.application.dto.response.ProjectFlowStats;
import com.tissue.feature.project.application.dto.response.ProjectMemberStats;
import com.tissue.feature.project.application.dto.response.ProjectSimpleStats;
import com.tissue.feature.project.application.dto.response.ProjectSprintReport;
import com.tissue.feature.project.application.dto.response.ProjectVelocity;
import com.tissue.feature.project.application.port.usecase.ProjectStatsQueryUseCase;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.sprint.application.service.SprintFinder;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only project statistics. Separated from {@code ProjectQueryService} so the statistics query
 * mechanism and its authorization policy live apart from plain project lookups.
 */
@LLMGenerated(llmInvolvement = LLMInvolvement.ASSISTED, model = "claude-opus-4-8")
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectStatsQueryService implements ProjectStatsQueryUseCase {

    // the contribution heatmap caps its lookback at a year so a caller cannot request an unbounded series
    private static final int MAX_CONTRIBUTION_DAYS = 366;

    private final ProjectFinder projectFinder;
    private final MemberFinder memberFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueStatsQueryRepository issueStatsQueryRepository;
    private final SprintFinder sprintFinder;

    @Override
    public ProjectSimpleStats getProjectSimpleStats(ProjectIdentifier pid, Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        ensureCanPreviewSimpleStats(project, actorMemberId);
        Long projectId = project.getId();

        Map<StateCategory, Long> byCategory = issueStatsQueryRepository.countByStateCategory(projectId).stream()
                .collect(Collectors.toMap(StateCategoryCountRow::getCategory, StateCategoryCountRow::getCount));
        Map<IssueHierarchy, Long> byHierarchy = issueStatsQueryRepository.countByHierarchy(projectId).stream()
                .collect(Collectors.toMap(HierarchyCountRow::getHierarchy, HierarchyCountRow::getCount));
        Map<IssuePriority, Long> byPriority = issueStatsQueryRepository.countByPriority(projectId).stream()
                .collect(Collectors.toMap(PriorityCountRow::getPriority, PriorityCountRow::getCount));
        ProjectStatsKpiRow kpi =
                issueStatsQueryRepository.getKpis(projectId, Instant.now(), StateCategory.terminalCategories());

        return ProjectSimpleStats.of(byCategory, byHierarchy, byPriority, kpi.getUnassigned(), kpi.getOverdue());
    }

    @Override
    public List<ProjectMemberStats> getProjectMemberStats(ProjectIdentifier pid, Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        ensureCanViewStats(project, actorMemberId);

        return issueStatsQueryRepository
                .getMemberStats(
                        project.getId(), StateCategory.COMPLETED, List.of(StateCategory.INITIAL, StateCategory.ACTIVE))
                .stream()
                .map(ProjectMemberStats::from)
                .toList();
    }

    @Override
    public ProjectAgingStats getProjectAgingStats(ProjectIdentifier pid, Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        ensureCanViewStats(project, actorMemberId);
        Long projectId = project.getId();

        Instant now = Instant.now();
        List<StateCategory> openCategories = List.of(StateCategory.INITIAL, StateCategory.ACTIVE);
        ProjectAgingRow buckets = issueStatsQueryRepository.getAgingBuckets(
                projectId,
                now.minus(Duration.ofDays(3)),
                now.minus(Duration.ofDays(7)),
                now.minus(Duration.ofDays(14)),
                openCategories);
        long blocked = issueStatsQueryRepository.countBlockedOpen(projectId, openCategories);

        return ProjectAgingStats.of(buckets, blocked);
    }

    @Override
    public ProjectFlowStats getProjectFlowStats(
            ProjectIdentifier pid,
            String window,
            @Nullable Long sprintId,
            @Nullable String zoneId,
            Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        ensureCanViewStats(project, actorMemberId);

        StatsWindowType type = StatsWindowType.fromOrDefault(window);
        StatsWindow win = resolveWindow(type, sprintId, project, Instant.now());

        List<Instant> created = issueStatsQueryRepository.findCreatedAtBetween(project.getId(), win.from(), win.to());
        List<Instant> resolved = issueStatsQueryRepository.findResolvedAtBetween(
                project.getId(), win.from(), win.to(), StateCategory.COMPLETED);

        return buildFlow(type, win, created, resolved, resolveZone(zoneId));
    }

    private StatsWindow resolveWindow(StatsWindowType type, @Nullable Long sprintId, Project project, Instant now) {
        return switch (type) {
            case WEEK -> StatsWindow.lastDays(7, now);
            case MONTH -> StatsWindow.lastDays(30, now);
            case SPRINT -> {
                if (sprintId == null) {
                    // a sprint window needs a sprint; without one, fall back to the rolling month
                    yield StatsWindow.lastDays(30, now);
                }
                Sprint sprint = sprintFinder.getBy(sprintId, project);
                if (sprint.getStartedAt() == null) {
                    // a sprint that never started has no active period to chart
                    yield StatsWindow.between(now, now);
                }
                Instant from = sprint.getStartedAt();
                Instant to = sprintWindowEnd(sprint, now);
                yield StatsWindow.between(from, to.isBefore(from) ? from : to);
            }
        };
    }

    // sprintWindowEnd is when the sprint stopped running: its completion instant if completed, or the
    // planned due date if it was cancelled (there is no recorded cancel instant, so the window is bounded to
    // the planned end rather than running to today), and "now" while it is still active - a live burn-up.
    private Instant sprintWindowEnd(Sprint sprint, Instant now) {
        if (sprint.getCompletedAt() != null) {
            return sprint.getCompletedAt();
        }
        if (sprint.isCancelled()) {
            return sprint.getDueAt() != null ? sprint.getDueAt() : now;
        }
        return now;
    }

    // buildFlow turns the raw creation/resolution instants into a dense, one-per-UTC-day series, zero-filling
    // days with no activity so the client can render a continuous trend without reconstructing the calendar.
    private ProjectFlowStats buildFlow(
            StatsWindowType type, StatsWindow win, List<Instant> created, List<Instant> resolved, ZoneId zone) {
        Map<LocalDate, Long> createdByDay = countByDay(created, zone);
        Map<LocalDate, Long> resolvedByDay = countByDay(resolved, zone);

        LocalDate start = win.from().atZone(zone).toLocalDate();
        LocalDate end = win.to().atZone(zone).toLocalDate();
        List<ProjectFlowStats.FlowPoint> points = new ArrayList<>();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            points.add(new ProjectFlowStats.FlowPoint(
                    day, createdByDay.getOrDefault(day, 0L), resolvedByDay.getOrDefault(day, 0L)));
        }
        return new ProjectFlowStats(win.from(), win.to(), type, points);
    }

    private Map<LocalDate, Long> countByDay(List<Instant> instants, ZoneId zone) {
        return instants.stream()
                .collect(Collectors.groupingBy(ts -> ts.atZone(zone).toLocalDate(), Collectors.counting()));
    }

    /**
     * Resolves the zone a day-bucketed series is cut on. Bucketing instants into days is a calendar
     * operation, so it needs a zone: cutting on UTC means a Seoul caller sees everything before local
     * mid-morning fall into the previous day. An unrecognized (or absent) zone falls back to UTC rather
     * than failing the read, mirroring how an unknown {@code window} falls back to a preset - a statistics
     * panel should still render.
     */
    private ZoneId resolveZone(@Nullable String zoneId) {
        if (zoneId == null || zoneId.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(zoneId);
        } catch (DateTimeException e) {
            return ZoneOffset.UTC;
        }
    }

    @Override
    public ProjectCycleTimeStats getProjectCycleTimeStats(
            ProjectIdentifier pid, String window, @Nullable Long sprintId, Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        ensureCanViewStats(project, actorMemberId);

        StatsWindowType type = StatsWindowType.fromOrDefault(window);
        StatsWindow win = resolveWindow(type, sprintId, project, Instant.now());
        Long projectId = project.getId();

        DurationStats cycle = DurationStats.of(durationsSeconds(issueStatsQueryRepository.findCycleTimePairs(
                projectId, win.from(), win.to(), StateCategory.COMPLETED)));
        DurationStats lead = DurationStats.of(durationsSeconds(
                issueStatsQueryRepository.findLeadTimePairs(projectId, win.from(), win.to(), StateCategory.COMPLETED)));

        return new ProjectCycleTimeStats(win.from(), win.to(), type, cycle, lead);
    }

    // durationsSeconds turns start/end pairs into non-negative second counts; a negative span (anomalous
    // clock/order) is clamped to zero so it cannot skew an average or percentile.
    private List<Long> durationsSeconds(List<TimestampPairRow> pairs) {
        return pairs.stream()
                .map(p -> Math.max(
                        0L, Duration.between(p.getStartAt(), p.getEndAt()).toSeconds()))
                .toList();
    }

    @Override
    public ProjectSprintReport getProjectSprintReport(ProjectIdentifier pid, Long sprintId, Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        ensureCanViewStats(project, actorMemberId);
        // getBy validates the sprint belongs to this project (404 otherwise) and gives us its metadata
        Sprint sprint = sprintFinder.getBy(sprintId, project);

        return ProjectSprintReport.of(sprint, issueStatsQueryRepository.getSprintStateAggregates(sprint.getId()));
    }

    @Override
    public ProjectVelocity getProjectVelocity(ProjectIdentifier pid, Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        ensureCanViewStats(project, actorMemberId);

        return ProjectVelocity.of(
                issueStatsQueryRepository.getVelocityBySprint(project.getId(), StateCategory.COMPLETED));
    }

    @Override
    public ProjectContributionStats getProjectContributions(
            ProjectIdentifier pid, Long memberId, int days, @Nullable String zoneId, Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        ensureCanViewStats(project, actorMemberId);

        // clamp the requested span so a caller cannot ask for an unbounded heatmap; the window is a whole
        // number of UTC days ending today (inclusive), so the series lines up on calendar-day boundaries
        ZoneId zone = resolveZone(zoneId);
        int span = Math.max(1, Math.min(days, MAX_CONTRIBUTION_DAYS));
        LocalDate endDay = Instant.now().atZone(zone).toLocalDate();
        LocalDate startDay = endDay.minusDays(span - 1L);
        Instant from = startDay.atStartOfDay(zone).toInstant();
        Instant to = endDay.plusDays(1).atStartOfDay(zone).toInstant();

        Map<LocalDate, Long> byDay = countByDay(
                issueStatsQueryRepository.findMemberResolvedAtBetween(
                        project.getId(), memberId, from, to, StateCategory.COMPLETED),
                zone);

        List<ContributionDay> series = new ArrayList<>();
        long total = 0;
        long max = 0;
        for (LocalDate day = startDay; !day.isAfter(endDay); day = day.plusDays(1)) {
            long count = byDay.getOrDefault(day, 0L);
            total += count;
            max = Math.max(max, count);
            series.add(new ContributionDay(day, count));
        }
        return new ProjectContributionStats(memberId, from, to, series, total, max);
    }

    // Deep stats expose per-member breakdowns and time-series internals, so they are limited to project
    // members - the same gate the issue data they aggregate uses. A non-member is rejected as if the
    // membership did not exist, consistent with the issue/comment/attachment read paths.
    private void ensureCanViewStats(Project project, Long actorMemberId) {
        projectMemberFinder.getBy(project, actorMemberId);
    }

    // simple-stats is the browse-time preview shown before joining: a PUBLIC project's coarse counts are
    // visible to any active member (so members can size up a project to join), while a PRIVATE project's
    // counts stay limited to its members.
    private void ensureCanPreviewSimpleStats(Project project, Long actorMemberId) {
        if (project.isPublic()) {
            memberFinder.getActiveById(actorMemberId);
            return;
        }
        projectMemberFinder.getBy(project, actorMemberId);
    }
}
