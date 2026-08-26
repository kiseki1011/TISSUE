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
import com.tissue.feature.project.application.dto.response.ProjectCycleTimeStats;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.ProjectStatsQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
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
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectCycleTimeStatsIntegrationTest extends IntegrationTestSupport {

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

    private Member actor;
    private Project project;

    private WorkflowState done;

    private IssueType standardType;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        project = projectRepository.save(Project.create("PROJ", "Proj", null));
        projectMemberRepository.save(ProjectMember.create(project, actor));

        Workflow workflow = Workflow.create(Name.of("CT WF"), null, ColorType.ANSI_YELLOW);
        workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("DOING"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflowRepository.save(workflow);

        standardType = issueTypeRepository.save(IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow));

        em.flush();
    }

    @Test
    @DisplayName("computes cycle time (start to resolve) and lead time (create to resolve) for one issue")
    void computesCycleAndLeadTimeForOneIssue() {
        // given - resolved 1 day ago, started 2h before that, created 5h before that
        Instant resolved = Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(1, ChronoUnit.DAYS);
        Long id = saveIssue(done).getId();
        setTimes(id, resolved.minus(5, ChronoUnit.HOURS), resolved.minus(2, ChronoUnit.HOURS), resolved);

        // when
        ProjectCycleTimeStats stats = cycleTimeOf("week", null);

        // then
        assertThat(stats.cycleTime().count()).isEqualTo(1);
        assertThat(stats.cycleTime().avgSeconds()).isEqualTo(2 * 3600);
        assertThat(stats.cycleTime().p50Seconds()).isEqualTo(2 * 3600);
        assertThat(stats.leadTime().count()).isEqualTo(1);
        assertThat(stats.leadTime().avgSeconds()).isEqualTo(5 * 3600);
    }

    @Test
    @DisplayName("computes avg/p50/p90 across several issues by interpolation")
    void computesPercentilesAcrossIssues() {
        // given - four issues with cycle durations 1h, 2h, 3h, 4h, all resolved a day ago
        Instant resolved = Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(1, ChronoUnit.DAYS);
        Instant created = resolved.minus(10, ChronoUnit.HOURS);
        for (int hours = 1; hours <= 4; hours++) {
            Long id = saveIssue(done).getId();
            setTimes(id, created, resolved.minus(hours, ChronoUnit.HOURS), resolved);
        }

        // when
        ProjectCycleTimeStats stats = cycleTimeOf("week", null);

        // then - sorted [3600, 7200, 10800, 14400]: avg 9000, p50 9000 (interp), p90 13320 (interp)
        assertThat(stats.cycleTime().count()).isEqualTo(4);
        assertThat(stats.cycleTime().avgSeconds()).isEqualTo(9000);
        assertThat(stats.cycleTime().p50Seconds()).isEqualTo(9000);
        assertThat(stats.cycleTime().p90Seconds()).isEqualTo(13320);
    }

    @Test
    @DisplayName("excludes issues resolved outside the window")
    void excludesIssuesResolvedOutsideWindow() {
        // given - resolved 40 days ago, outside the 7-day window
        Instant resolved = Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(40, ChronoUnit.DAYS);
        Long id = saveIssue(done).getId();
        setTimes(id, resolved.minus(5, ChronoUnit.HOURS), resolved.minus(2, ChronoUnit.HOURS), resolved);

        // when
        ProjectCycleTimeStats stats = cycleTimeOf("week", null);

        // then
        assertThat(stats.cycleTime().count()).isZero();
        assertThat(stats.leadTime().count()).isZero();
    }

    @Test
    @DisplayName("cycle time excludes issues that never started, but lead time still counts them")
    void cycleTimeExcludesNeverStarted() {
        // given - a resolved issue that never started (no startedAt)
        Instant resolved = Instant.now().truncatedTo(ChronoUnit.SECONDS).minus(1, ChronoUnit.DAYS);
        Long id = saveIssue(done).getId();
        setTimes(id, resolved.minus(5, ChronoUnit.HOURS), null, resolved);

        // when
        ProjectCycleTimeStats stats = cycleTimeOf("week", null);

        // then
        assertThat(stats.cycleTime().count()).isZero();
        assertThat(stats.leadTime().count()).isEqualTo(1);
        assertThat(stats.leadTime().avgSeconds()).isEqualTo(5 * 3600);
    }

    @Test
    @DisplayName("returns zeroed stats when nothing was resolved in the window")
    void returnsZeroedStatsWhenEmpty() {
        // when
        ProjectCycleTimeStats stats = cycleTimeOf("month", null);

        // then
        assertThat(stats.cycleTime().count()).isZero();
        assertThat(stats.cycleTime().avgSeconds()).isZero();
        assertThat(stats.cycleTime().p50Seconds()).isZero();
        assertThat(stats.cycleTime().p90Seconds()).isZero();
        assertThat(stats.leadTime().count()).isZero();
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

    private void setTimes(Long issueId, Instant createdAt, @Nullable Instant startedAt, Instant resolvedAt) {
        em.createNativeQuery("UPDATE issue SET created_at = :c, resolved_at = :r WHERE id = :id")
                .setParameter("c", createdAt)
                .setParameter("r", resolvedAt)
                .setParameter("id", issueId)
                .executeUpdate();
        if (startedAt != null) {
            em.createNativeQuery("UPDATE issue SET started_at = :s WHERE id = :id")
                    .setParameter("s", startedAt)
                    .setParameter("id", issueId)
                    .executeUpdate();
        } else {
            em.createNativeQuery("UPDATE issue SET started_at = NULL WHERE id = :id")
                    .setParameter("id", issueId)
                    .executeUpdate();
        }
        em.flush();
        em.clear();
    }

    private ProjectCycleTimeStats cycleTimeOf(String window, Long sprintId) {
        return sut.getProjectCycleTimeStats(ProjectIdentifier.ofProjectKey("PROJ"), window, sprintId, actor.getId());
    }
}
