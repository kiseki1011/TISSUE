package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.issue.application.port.repository.IssueCommandRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueContent;
import com.tissue.feature.issue.domain.IssueParticipants;
import com.tissue.feature.issue.domain.IssueSchedule;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectFlowStats;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.ProjectStatsQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.sprint.application.port.repository.SprintCommandRepository;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectFlowStatsIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private ProjectStatsQueryService sut;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @Autowired
    private IssueCommandRepository issueCommandRepository;

    @Autowired
    private SprintCommandRepository sprintRepository;

    private Member actor;
    private Project project;

    private WorkflowState todo;
    private WorkflowState done;
    private WorkflowState cancelled;

    private IssueType standardType;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        project = projectRepository.save(Project.create("PROJ", "Proj", null));
        projectMemberRepository.save(ProjectMember.create(project, actor));

        Workflow workflow = Workflow.create(Name.of("Flow WF"), null, ColorType.ANSI_YELLOW);
        todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("DOING"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        cancelled = workflow.addState(Name.of("CANCELLED"), null, ColorType.ANSI_RED, StateCategory.ABORTED);
        workflowRepository.save(workflow);

        standardType = issueTypeRepository.save(IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow));

        em.flush();
    }

    @Test
    @DisplayName("buckets created issues per UTC day and zero-fills the rest of the window")
    void bucketsCreatedByDay() {
        // given
        Instant now = Instant.now();
        setCreatedAt(saveIssue(todo).getId(), now.minus(1, ChronoUnit.DAYS));
        setCreatedAt(saveIssue(todo).getId(), now.minus(1, ChronoUnit.DAYS));
        setCreatedAt(saveIssue(todo).getId(), now.minus(3, ChronoUnit.DAYS));

        // when
        ProjectFlowStats flow = flowOf("week", null);

        // then - a dense 8-point series (7-day span, inclusive) with the counts on the right days
        assertThat(flow.points()).hasSize(8);
        assertThat(pointFor(flow, utcDay(now.minus(1, ChronoUnit.DAYS))).created())
                .isEqualTo(2);
        assertThat(pointFor(flow, utcDay(now.minus(3, ChronoUnit.DAYS))).created())
                .isEqualTo(1);
        assertThat(flow.points().stream()
                        .mapToLong(ProjectFlowStats.FlowPoint::created)
                        .sum())
                .isEqualTo(3);
        assertThat(flow.points().stream()
                        .mapToLong(ProjectFlowStats.FlowPoint::resolved)
                        .sum())
                .isZero();
    }

    @Test
    @DisplayName("buckets resolved issues by their resolution day, counting only currently completed ones")
    void bucketsResolvedByDayCompletedOnly() {
        // given - a completed and an aborted issue resolved on the same day; created far before the window
        Instant now = Instant.now();
        Issue completed = saveIssue(done);
        setCreatedAt(completed.getId(), now.minus(40, ChronoUnit.DAYS));
        setResolvedAt(completed.getId(), now.minus(3, ChronoUnit.DAYS));
        Issue aborted = saveIssue(cancelled);
        setCreatedAt(aborted.getId(), now.minus(40, ChronoUnit.DAYS));
        setResolvedAt(aborted.getId(), now.minus(3, ChronoUnit.DAYS));

        // when
        ProjectFlowStats flow = flowOf("week", null);

        // then - only the completed issue counts, and nothing leaks into the created series
        assertThat(pointFor(flow, utcDay(now.minus(3, ChronoUnit.DAYS))).resolved())
                .isEqualTo(1);
        assertThat(flow.points().stream()
                        .mapToLong(ProjectFlowStats.FlowPoint::resolved)
                        .sum())
                .isEqualTo(1);
        assertThat(flow.points().stream()
                        .mapToLong(ProjectFlowStats.FlowPoint::created)
                        .sum())
                .isZero();
    }

    @Test
    @DisplayName("excludes events created outside the window")
    void excludesEventsOutsideWindow() {
        // given
        Instant now = Instant.now();
        setCreatedAt(saveIssue(todo).getId(), now.minus(2, ChronoUnit.DAYS)); // inside the 7-day window
        setCreatedAt(saveIssue(todo).getId(), now.minus(40, ChronoUnit.DAYS)); // outside

        // when
        ProjectFlowStats flow = flowOf("week", null);

        // then
        assertThat(flow.points().stream()
                        .mapToLong(ProjectFlowStats.FlowPoint::created)
                        .sum())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a sprint window spans the sprint's started~completed period")
    void sprintWindowSpansSprintPeriod() {
        // given
        Instant now = Instant.now();
        Sprint sprint = sprintRepository.save(Sprint.create(project, "S1", null));
        em.flush();
        setSprintPeriod(sprint.getId(), now.minus(10, ChronoUnit.DAYS), now.minus(2, ChronoUnit.DAYS));
        setCreatedAt(saveIssue(todo).getId(), now.minus(5, ChronoUnit.DAYS)); // within the sprint period
        setCreatedAt(saveIssue(todo).getId(), now.minus(15, ChronoUnit.DAYS)); // before it started

        // when
        ProjectFlowStats flow = flowOf("sprint", sprint.getId());

        // then
        assertThat(flow.points().stream()
                        .mapToLong(ProjectFlowStats.FlowPoint::created)
                        .sum())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("a sprint cancelled while active ends the window at its planned due date, not today")
    void cancelledSprintWindowStopsAtDueDate() {
        // given - a sprint that started 10 days ago, was due 5 days ago, and was cancelled (no completedAt)
        Instant now = Instant.now();
        Sprint sprint = sprintRepository.save(Sprint.create(project, "S1", null));
        em.flush();
        cancelSprint(sprint.getId(), now.minus(10, ChronoUnit.DAYS), now.minus(5, ChronoUnit.DAYS));
        setCreatedAt(saveIssue(todo).getId(), now.minus(7, ChronoUnit.DAYS)); // during the sprint's period
        setCreatedAt(saveIssue(todo).getId(), now.minus(3, ChronoUnit.DAYS)); // after it was due (post-cancel)

        // when
        ProjectFlowStats flow = flowOf("sprint", sprint.getId());

        // then - the window ends at the due date, so the post-cancel issue is excluded
        assertThat(flow.to()).isBefore(now.minus(4, ChronoUnit.DAYS));
        assertThat(flow.points().stream()
                        .mapToLong(ProjectFlowStats.FlowPoint::created)
                        .sum())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("returns a dense zero-filled series for a project with no activity")
    void emptyProjectReturnsDenseZeroSeries() {
        // when
        ProjectFlowStats flow = flowOf("month", null);

        // then - 30-day span, inclusive => 31 points, all zero
        assertThat(flow.points()).hasSize(31);
        assertThat(flow.points()).allMatch(p -> p.created() == 0 && p.resolved() == 0);
    }

    private Issue saveIssue(WorkflowState targetState) {
        Issue issue = Issue.create(
                project,
                null,
                standardType,
                "issue",
                IssueContent.of("c", "s"),
                IssueSchedule.of(null),
                IssueParticipants.of(null),
                IssuePriority.P2,
                null,
                null);
        if (!targetState.getCategory().isInitial()) {
            issue.transitionTo(targetState);
        }
        Issue saved = issueCommandRepository.save(issue);
        em.flush();
        return saved;
    }

    private void setCreatedAt(Long issueId, Instant ts) {
        nativeSet("issue", "created_at", ts, issueId);
    }

    private void setResolvedAt(Long issueId, Instant ts) {
        nativeSet("issue", "resolved_at", ts, issueId);
    }

    private void setSprintPeriod(Long sprintId, Instant startedAt, Instant completedAt) {
        em.createNativeQuery("UPDATE sprint SET started_at = :s, completed_at = :c WHERE id = :id")
                .setParameter("s", startedAt)
                .setParameter("c", completedAt)
                .setParameter("id", sprintId)
                .executeUpdate();
        em.flush();
        em.clear();
    }

    private void cancelSprint(Long sprintId, Instant startedAt, Instant dueAt) {
        em.createNativeQuery(
                        "UPDATE sprint SET started_at = :s, due_at = :d, sprint_status = 'CANCELLED' WHERE id = :id")
                .setParameter("s", startedAt)
                .setParameter("d", dueAt)
                .setParameter("id", sprintId)
                .executeUpdate();
        em.flush();
        em.clear();
    }

    private void nativeSet(String table, String column, Instant ts, Long id) {
        em.createNativeQuery("UPDATE " + table + " SET " + column + " = :ts WHERE id = :id")
                .setParameter("ts", ts)
                .setParameter("id", id)
                .executeUpdate();
        em.flush();
        em.clear();
    }

    private LocalDate utcDay(Instant t) {
        return t.atZone(ZoneOffset.UTC).toLocalDate();
    }

    private ProjectFlowStats.FlowPoint pointFor(ProjectFlowStats flow, LocalDate day) {
        return flow.points().stream()
                .filter(p -> p.date().equals(day))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no point for " + day));
    }

    private ProjectFlowStats flowOf(String window, Long sprintId) {
        return sut.getProjectFlowStats(ProjectIdentifier.ofProjectKey("PROJ"), window, sprintId, null, actor.getId());
    }
}
