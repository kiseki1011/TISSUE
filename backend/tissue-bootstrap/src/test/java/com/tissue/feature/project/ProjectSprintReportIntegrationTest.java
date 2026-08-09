package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.tissue.feature.project.application.dto.response.ProjectSprintReport;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.ProjectStatsQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.sprint.application.port.repository.SprintCommandRepository;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.feature.sprint.domain.exception.SprintNotFoundException;
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
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectSprintReportIntegrationTest extends IntegrationTestSupport {

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
    private WorkflowState doing;
    private WorkflowState done;
    private WorkflowState cancelled;

    private IssueType standardType;
    private IssueType epicType;

    private Sprint sprint;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        project = projectRepository.save(Project.create("PROJ", "Proj", null));
        projectMemberRepository.save(ProjectMember.create(project, actor));

        Workflow workflow = Workflow.create(Name.of("Flow WF"), null, ColorType.ANSI_YELLOW);
        todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        doing = workflow.addState(Name.of("DOING"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        cancelled = workflow.addState(Name.of("CANCELLED"), null, ColorType.ANSI_RED, StateCategory.ABORTED);
        workflowRepository.save(workflow);

        standardType = issueTypeRepository.save(IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow));
        epicType = issueTypeRepository.save(IssueType.create(
                Name.of("Epic"), null, ColorType.ANSI_MAGENTA, IconType.CIRCLE_FILLED, IssueHierarchy.EPIC, workflow));

        sprint = sprintRepository.save(Sprint.create(project, "Sprint 1", "Ship the thing"));
        em.flush();
    }

    @Test
    @DisplayName("reports scope counts, completion rate, the full state distribution and sprint metadata")
    void reportsCountsCompletionRateAndDistribution() {
        // given - 2 completed, 1 active, 1 initial, 1 aborted in the sprint
        saveIssue(standardType, done, null, sprint);
        saveIssue(standardType, done, null, sprint);
        saveIssue(standardType, doing, null, sprint);
        saveIssue(standardType, todo, null, sprint);
        saveIssue(standardType, cancelled, null, sprint);

        // when
        ProjectSprintReport report = reportOf(sprint.getId());

        // then - counts
        assertThat(report.totalIssues()).isEqualTo(5);
        assertThat(report.completedIssues()).isEqualTo(2);
        assertThat(report.openIssues()).isEqualTo(2); // initial + active; aborted is neither
        assertThat(report.completionRate()).isEqualTo(2.0 / 5.0);

        // then - distribution has every non-empty category
        Map<StateCategory, Long> dist = report.stateDistribution().stream()
                .collect(Collectors.toMap(
                        ProjectSprintReport.SprintStateCount::category, ProjectSprintReport.SprintStateCount::count));
        assertThat(dist)
                .containsEntry(StateCategory.COMPLETED, 2L)
                .containsEntry(StateCategory.ACTIVE, 1L)
                .containsEntry(StateCategory.INITIAL, 1L)
                .containsEntry(StateCategory.ABORTED, 1L);

        // then - metadata mirrors the sprint
        assertThat(report.sprintId()).isEqualTo(sprint.getId());
        assertThat(report.sprintKey()).isEqualTo(sprint.getSprintKey());
        assertThat(report.title()).isEqualTo("Sprint 1");
        assertThat(report.goal()).isEqualTo("Ship the thing");
        assertThat(report.status()).isEqualTo(SprintStatus.PLANNING);
    }

    @Test
    @DisplayName("story points sum over the sprint's issues but exclude EPIC rollups")
    void storyPointsExcludeEpicRollups() {
        // given - a completed STANDARD story worth 5 points and an active EPIC whose rolled-up 8 points
        // should NOT be counted (its child stories would be counted directly if they were in the sprint)
        saveIssue(standardType, done, 5, sprint);
        Issue epic = saveIssue(epicType, doing, null, sprint);
        setStoryPoint(epic.getId(), 8);

        // when
        ProjectSprintReport report = reportOf(sprint.getId());

        // then
        assertThat(report.totalIssues()).isEqualTo(2);
        assertThat(report.completedIssues()).isEqualTo(1);
        assertThat(report.openIssues()).isEqualTo(1);
        assertThat(report.totalStoryPoints()).isEqualTo(5); // epic's 8 excluded
        assertThat(report.completedStoryPoints()).isEqualTo(5);
        assertThat(report.pointsCompletionRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("counts only issues assigned to this sprint")
    void onlyCountsIssuesAssignedToThisSprint() {
        // given
        Sprint other = sprintRepository.save(Sprint.create(project, "Sprint 2", null));
        em.flush();
        saveIssue(standardType, done, null, sprint); // in this sprint
        saveIssue(standardType, done, null, other); // in another sprint
        saveIssue(standardType, done, null, null); // backlog, no sprint

        // when
        ProjectSprintReport report = reportOf(sprint.getId());

        // then
        assertThat(report.totalIssues()).isEqualTo(1);
        assertThat(report.completedIssues()).isEqualTo(1);
    }

    @Test
    @DisplayName("excludes soft-deleted issues")
    void excludesSoftDeletedIssues() {
        // given
        saveIssue(standardType, done, null, sprint);
        Issue removed = saveIssue(standardType, doing, null, sprint);
        softDelete(removed.getId());

        // when
        ProjectSprintReport report = reportOf(sprint.getId());

        // then
        assertThat(report.totalIssues()).isEqualTo(1);
        assertThat(report.openIssues()).isZero();
        assertThat(report.completedIssues()).isEqualTo(1);
    }

    @Test
    @DisplayName("returns all-zero stats for a sprint with no issues")
    void emptySprintReturnsAllZeros() {
        // when
        ProjectSprintReport report = reportOf(sprint.getId());

        // then
        assertThat(report.totalIssues()).isZero();
        assertThat(report.completedIssues()).isZero();
        assertThat(report.openIssues()).isZero();
        assertThat(report.completionRate()).isZero();
        assertThat(report.totalStoryPoints()).isZero();
        assertThat(report.completedStoryPoints()).isZero();
        assertThat(report.pointsCompletionRate()).isZero();
        assertThat(report.stateDistribution()).isEmpty();
    }

    @Test
    @DisplayName("surfaces the sprint's status and lifecycle instants (started/due/completed) in their own slots")
    void surfacesSprintLifecycleMetadata() {
        // given - a completed sprint with three DISTINCT known instants, so a positional mix-up of the three
        // adjacent nullable Instant fields in ProjectSprintReport.of would show up as a wrong value here
        Instant started = Instant.now().minus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
        Instant due = Instant.now().minus(3, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
        Instant completed = Instant.now().minus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
        setLifecycle(sprint.getId(), started, due, completed, "COMPLETED");

        // when
        ProjectSprintReport report = reportOf(sprint.getId());

        // then
        assertThat(report.status()).isEqualTo(SprintStatus.COMPLETED);
        assertThat(report.startedAt()).isEqualTo(started);
        assertThat(report.dueAt()).isEqualTo(due);
        assertThat(report.completedAt()).isEqualTo(completed);
    }

    @Test
    @DisplayName("throws when the sprint does not belong to the project")
    void throwsWhenSprintNotInProject() {
        // given - a sprint under a different project
        Project otherProject = projectRepository.save(Project.create("OTHR", "Other", null));
        Sprint foreignSprint = sprintRepository.save(Sprint.create(otherProject, "Foreign", null));
        em.flush();

        // when / then
        assertThatThrownBy(() -> reportOf(foreignSprint.getId())).isInstanceOf(SprintNotFoundException.class);
    }

    private Issue saveIssue(IssueType type, WorkflowState state, Integer storyPoint, Sprint sprintOrNull) {
        Issue issue = Issue.create(
                project,
                sprintOrNull,
                type,
                "issue",
                IssueContent.of("c", "s"),
                IssueSchedule.of(null),
                IssueParticipants.of(null),
                IssuePriority.P2,
                storyPoint,
                null);
        if (!state.getCategory().isInitial()) {
            issue.transitionTo(state);
        }
        Issue saved = issueCommandRepository.save(issue);
        em.flush();
        return saved;
    }

    private void setStoryPoint(Long issueId, int storyPoint) {
        em.createNativeQuery("UPDATE issue SET story_point = :sp WHERE id = :id")
                .setParameter("sp", storyPoint)
                .setParameter("id", issueId)
                .executeUpdate();
        em.flush();
        em.clear();
    }

    private void setLifecycle(Long sprintId, Instant startedAt, Instant dueAt, Instant completedAt, String status) {
        em.createNativeQuery("UPDATE sprint SET started_at = :s, due_at = :d, completed_at = :c,"
                        + " sprint_status = :st WHERE id = :id")
                .setParameter("s", startedAt)
                .setParameter("d", dueAt)
                .setParameter("c", completedAt)
                .setParameter("st", status)
                .setParameter("id", sprintId)
                .executeUpdate();
        em.flush();
        em.clear();
    }

    private void softDelete(Long issueId) {
        em.createNativeQuery("UPDATE issue SET soft_deleted = true WHERE id = :id")
                .setParameter("id", issueId)
                .executeUpdate();
        em.flush();
        em.clear();
    }

    private ProjectSprintReport reportOf(Long sprintId) {
        return sut.getProjectSprintReport(ProjectIdentifier.ofProjectKey("PROJ"), sprintId, actor.getId());
    }
}
