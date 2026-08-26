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
import com.tissue.feature.project.application.dto.response.ProjectSimpleStats;
import com.tissue.feature.project.application.dto.response.ProjectSimpleStats.CategoryCount;
import com.tissue.feature.project.application.dto.response.ProjectSimpleStats.HierarchyCount;
import com.tissue.feature.project.application.dto.response.ProjectSimpleStats.PriorityCount;
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
class ProjectSimpleStatsIntegrationTest extends IntegrationTestSupport {

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
    private ProjectMember assigneeMember;

    private WorkflowState todo;
    private WorkflowState active;
    private WorkflowState done;
    private WorkflowState cancelled;

    private IssueType epicType;
    private IssueType standardType;
    private IssueType subtaskType;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        project = projectRepository.save(Project.create("PROJ", "Proj", null));
        assigneeMember = projectMemberRepository.save(ProjectMember.create(project, actor));

        Workflow workflow = Workflow.create(Name.of("Stats WF"), null, ColorType.ANSI_YELLOW);
        todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        active = workflow.addState(Name.of("DOING"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        cancelled = workflow.addState(Name.of("CANCELLED"), null, ColorType.ANSI_RED, StateCategory.ABORTED);
        workflowRepository.save(workflow);

        epicType = saveType("Epic", IssueHierarchy.EPIC, workflow);
        standardType = saveType("Story", IssueHierarchy.STANDARD, workflow);
        subtaskType = saveType("Sub", IssueHierarchy.SUBTASK, workflow);

        em.flush();
    }

    @Test
    @DisplayName("counts issues per state category with all four buckets")
    void countsIssuesPerStateCategory() {
        // given
        saveIssue(standardType, IssuePriority.P2, null, todo);
        saveIssue(standardType, IssuePriority.P2, null, active);
        saveIssue(standardType, IssuePriority.P2, null, active);
        saveIssue(standardType, IssuePriority.P2, null, done);
        saveIssue(standardType, IssuePriority.P2, null, cancelled);

        // when
        ProjectSimpleStats stats = statsOf("PROJ");

        // then
        assertThat(stats.byStateCategory())
                .containsExactly(
                        new CategoryCount(StateCategory.INITIAL, 1),
                        new CategoryCount(StateCategory.ACTIVE, 2),
                        new CategoryCount(StateCategory.COMPLETED, 1),
                        new CategoryCount(StateCategory.ABORTED, 1));
    }

    @Test
    @DisplayName("derives total, open and completed from state categories")
    void derivesTotalOpenCompleted() {
        // given
        saveIssue(standardType, IssuePriority.P2, null, todo);
        saveIssue(standardType, IssuePriority.P2, null, active);
        saveIssue(standardType, IssuePriority.P2, null, done);
        saveIssue(standardType, IssuePriority.P2, null, cancelled);

        // when
        ProjectSimpleStats stats = statsOf("PROJ");

        // then
        assertThat(stats.total()).isEqualTo(4);
        assertThat(stats.open()).isEqualTo(2);
        assertThat(stats.completed()).isEqualTo(1);
    }

    @Test
    @DisplayName("counts issues per hierarchy and zero-fills the missing bucket")
    void countsIssuesPerHierarchyWithZeroFill() {
        // given
        saveIssue(epicType, IssuePriority.P2, null, todo);
        saveIssue(standardType, IssuePriority.P2, null, todo);
        saveIssue(standardType, IssuePriority.P2, null, todo);
        saveIssue(subtaskType, IssuePriority.P2, null, todo);

        // when
        ProjectSimpleStats stats = statsOf("PROJ");

        // then
        assertThat(stats.byHierarchy())
                .containsExactly(
                        new HierarchyCount(IssueHierarchy.EPIC, 1),
                        new HierarchyCount(IssueHierarchy.STANDARD, 2),
                        new HierarchyCount(IssueHierarchy.SUBTASK, 1),
                        new HierarchyCount(IssueHierarchy.MICROTASK, 0));
    }

    @Test
    @DisplayName("counts issues per priority and zero-fills the missing buckets")
    void countsIssuesPerPriorityWithZeroFill() {
        // given
        saveIssue(standardType, IssuePriority.P0, null, todo);
        saveIssue(standardType, IssuePriority.P2, null, todo);
        saveIssue(standardType, IssuePriority.P2, null, todo);

        // when
        ProjectSimpleStats stats = statsOf("PROJ");

        // then
        assertThat(stats.byPriority())
                .containsExactly(
                        new PriorityCount(IssuePriority.P0, 1),
                        new PriorityCount(IssuePriority.P1, 0),
                        new PriorityCount(IssuePriority.P2, 2),
                        new PriorityCount(IssuePriority.P3, 0),
                        new PriorityCount(IssuePriority.P4, 0));
    }

    @Test
    @DisplayName("counts issues that have no assignee")
    void countsUnassignedIssues() {
        // given
        saveIssue(standardType, IssuePriority.P2, assigneeMember, todo);
        saveIssue(standardType, IssuePriority.P2, null, todo);
        saveIssue(standardType, IssuePriority.P2, null, todo);

        // when
        ProjectSimpleStats stats = statsOf("PROJ");

        // then
        assertThat(stats.unassigned()).isEqualTo(2);
    }

    @Test
    @DisplayName("counts overdue open issues but excludes completed ones")
    void countsOverdueOpenIssues() {
        // given
        Issue overdueOpen = saveIssue(standardType, IssuePriority.P2, null, todo);
        Issue overdueDone = saveIssue(standardType, IssuePriority.P2, null, done);
        saveIssue(standardType, IssuePriority.P2, null, todo);
        Instant past = Instant.now().minus(2, ChronoUnit.DAYS);
        backdateDue(overdueOpen, past);
        backdateDue(overdueDone, past);

        // when
        ProjectSimpleStats stats = statsOf("PROJ");

        // then
        assertThat(stats.overdue()).isEqualTo(1);
    }

    @Test
    @DisplayName("excludes soft deleted issues from the counts")
    void excludesSoftDeletedIssues() {
        // given
        saveIssue(standardType, IssuePriority.P2, null, todo);
        Issue deleted = saveIssue(standardType, IssuePriority.P2, null, todo);
        deleted.softDelete();
        issueCommandRepository.save(deleted);
        em.flush();

        // when
        ProjectSimpleStats stats = statsOf("PROJ");

        // then
        assertThat(stats.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("returns zeroed stats for a project with no issues")
    void returnsZeroedStatsForEmptyProject() {
        // when
        ProjectSimpleStats stats = statsOf("PROJ");

        // then
        assertThat(stats.total()).isZero();
        assertThat(stats.open()).isZero();
        assertThat(stats.completed()).isZero();
        assertThat(stats.unassigned()).isZero();
        assertThat(stats.overdue()).isZero();
        assertThat(stats.byStateCategory()).hasSize(4).allMatch(bucket -> bucket.count() == 0);
        assertThat(stats.byHierarchy()).hasSize(4).allMatch(bucket -> bucket.count() == 0);
        assertThat(stats.byPriority()).hasSize(5).allMatch(bucket -> bucket.count() == 0);
    }

    @Test
    @DisplayName("scopes the counts to the requested project")
    void scopesCountsToProject() {
        // given
        Project other = projectRepository.save(Project.create("TEAM", "Team", null));
        Issue teamIssue = Issue.create(
                other,
                null,
                standardType,
                "team-issue",
                IssueContent.of("c", "s"),
                IssueSchedule.of(null),
                IssueParticipants.of(null),
                IssuePriority.P2,
                null,
                null);
        issueCommandRepository.save(teamIssue);
        saveIssue(standardType, IssuePriority.P2, null, todo);
        em.flush();

        // when
        ProjectSimpleStats stats = statsOf("PROJ");

        // then
        assertThat(stats.total()).isEqualTo(1);
    }

    private IssueType saveType(String name, IssueHierarchy hierarchy, Workflow workflow) {
        return issueTypeRepository.save(
                IssueType.create(Name.of(name), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, hierarchy, workflow));
    }

    private Issue saveIssue(
            IssueType type, IssuePriority priority, @Nullable ProjectMember assignee, WorkflowState targetState) {
        Issue issue = Issue.create(
                project,
                null,
                type,
                "issue",
                IssueContent.of("c", "s"),
                IssueSchedule.of(null),
                IssueParticipants.of(assignee),
                priority,
                null,
                null);
        if (!targetState.getCategory().isInitial()) {
            issue.transitionTo(targetState);
        }
        Issue saved = issueCommandRepository.save(issue);
        em.flush();
        return saved;
    }

    private void backdateDue(Issue issue, Instant past) {
        em.createNativeQuery("UPDATE issue SET due_at = :dueAt WHERE id = :id")
                .setParameter("dueAt", past)
                .setParameter("id", issue.getId())
                .executeUpdate();
        em.flush();
        em.clear();
    }

    private ProjectSimpleStats statsOf(String key) {
        return sut.getProjectSimpleStats(ProjectIdentifier.ofProjectKey(key), actor.getId());
    }
}
