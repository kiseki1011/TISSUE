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
import com.tissue.feature.project.application.dto.response.ProjectVelocity;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.ProjectStatsQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
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
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectVelocityIntegrationTest extends IntegrationTestSupport {

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

    private WorkflowState doing;
    private WorkflowState done;

    private IssueType standardType;
    private IssueType epicType;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        project = projectRepository.save(Project.create("PROJ", "Proj", null));
        projectMemberRepository.save(ProjectMember.create(project, actor));

        Workflow workflow = Workflow.create(Name.of("Flow WF"), null, ColorType.ANSI_YELLOW);
        workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        doing = workflow.addState(Name.of("DOING"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflowRepository.save(workflow);

        standardType = issueTypeRepository.save(IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow));
        epicType = issueTypeRepository.save(IssueType.create(
                Name.of("Epic"), null, ColorType.ANSI_MAGENTA, IconType.CIRCLE_FILLED, IssueHierarchy.EPIC, workflow));
    }

    @Test
    @DisplayName("reports one point per COMPLETED sprint, oldest first, with EPIC points excluded")
    void reportsPointPerCompletedSprint() {
        // given - sprint 1 (completed): two done stories worth 5 and 3, plus one still doing
        Sprint s1 = completedSprint("Sprint 1", 5);
        saveIssue(standardType, done, 5, s1);
        saveIssue(standardType, done, 3, s1);
        saveIssue(standardType, doing, 8, s1);
        // sprint 2 (completed): one done story worth 2 and a done EPIC worth 10 (its rollup must not count).
        // EPIC points cannot be set through the domain (updateStoryPoint rejects EPIC), so set them natively.
        Sprint s2 = completedSprint("Sprint 2", 3);
        saveIssue(standardType, done, 2, s2);
        Issue epic = saveIssue(epicType, done, null, s2);
        setStoryPoint(epic.getId(), 10);
        // sprint 3 (still PLANNING): a done story that must be ignored - only completed sprints count
        Sprint planning = sprintRepository.save(Sprint.create(project, "Sprint 3", null));
        em.flush();
        saveIssue(standardType, done, 99, planning);

        // when
        ProjectVelocity velocity = velocity();

        // then - only the two completed sprints, in ascending sprint-number order
        assertThat(velocity.sprints()).hasSize(2);
        assertThat(velocity.sprints())
                .extracting(ProjectVelocity.VelocityPoint::sprintKey)
                .containsExactly(s1.getSprintKey(), s2.getSprintKey());

        ProjectVelocity.VelocityPoint p1 = velocity.sprints().get(0);
        assertThat(p1.completedIssues()).isEqualTo(2); // the doing story is not completed
        assertThat(p1.completedStoryPoints()).isEqualTo(8); // 5 + 3

        ProjectVelocity.VelocityPoint p2 = velocity.sprints().get(1);
        assertThat(p2.completedIssues()).isEqualTo(2); // story + epic both count as issues
        assertThat(p2.completedStoryPoints()).isEqualTo(2); // epic's 10 excluded

        // then - averages over the two returned sprints
        assertThat(velocity.averageStoryPoints()).isEqualTo((8 + 2) / 2.0);
        assertThat(velocity.averageCompletedIssues()).isEqualTo((2 + 2) / 2.0);
    }

    @Test
    @DisplayName("a completed sprint that delivered nothing still appears as a zero point")
    void completedSprintWithNoCompletedIssuesIsAZeroPoint() {
        // given - a completed sprint whose only issue is still in progress
        Sprint s1 = completedSprint("Sprint 1", 2);
        saveIssue(standardType, doing, 4, s1);

        // when
        ProjectVelocity velocity = velocity();

        // then
        assertThat(velocity.sprints()).hasSize(1);
        assertThat(velocity.sprints().get(0).completedIssues()).isZero();
        assertThat(velocity.sprints().get(0).completedStoryPoints()).isZero();
        assertThat(velocity.averageStoryPoints()).isZero();
    }

    @Test
    @DisplayName("returns an empty series and zero averages when there are no completed sprints")
    void emptyWhenNoCompletedSprints() {
        // given - a planning sprint with a done issue, but no completed sprint
        Sprint planning = sprintRepository.save(Sprint.create(project, "Sprint 1", null));
        em.flush();
        saveIssue(standardType, done, 5, planning);

        // when
        ProjectVelocity velocity = velocity();

        // then
        assertThat(velocity.sprints()).isEmpty();
        assertThat(velocity.averageStoryPoints()).isZero();
        assertThat(velocity.averageCompletedIssues()).isZero();
    }

    @Test
    @DisplayName("excludes soft-deleted issues from a sprint's velocity")
    void excludesSoftDeletedIssues() {
        // given
        Sprint s1 = completedSprint("Sprint 1", 2);
        saveIssue(standardType, done, 5, s1);
        Issue removed = saveIssue(standardType, done, 7, s1);
        softDelete(removed.getId());

        // when
        ProjectVelocity velocity = velocity();

        // then
        assertThat(velocity.sprints()).hasSize(1);
        assertThat(velocity.sprints().get(0).completedIssues()).isEqualTo(1);
        assertThat(velocity.sprints().get(0).completedStoryPoints()).isEqualTo(5);
    }

    @Test
    @DisplayName("rejects a non-member: velocity is member-only")
    void rejectsNonMember() {
        // given
        Member outsider = memberRepository.save(Member.create("out@tissue.com", "outsider", "Outsider"));
        em.flush();

        // when / then
        assertThatThrownBy(() -> sut.getProjectVelocity(ProjectIdentifier.ofProjectKey("PROJ"), outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    private Sprint completedSprint(String title, int daysAgoCompleted) {
        Sprint sprint = sprintRepository.save(Sprint.create(project, title, null));
        em.flush();
        Instant completedAt = Instant.now().minus(daysAgoCompleted, ChronoUnit.DAYS);
        em.createNativeQuery("UPDATE sprint SET sprint_status = 'COMPLETED', completed_at = :c WHERE id = :id")
                .setParameter("c", completedAt)
                .setParameter("id", sprint.getId())
                .executeUpdate();
        em.flush();
        em.clear();
        return sprint;
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

    private void softDelete(Long issueId) {
        em.createNativeQuery("UPDATE issue SET soft_deleted = true WHERE id = :id")
                .setParameter("id", issueId)
                .executeUpdate();
        em.flush();
        em.clear();
    }

    private ProjectVelocity velocity() {
        return sut.getProjectVelocity(ProjectIdentifier.ofProjectKey("PROJ"), actor.getId());
    }
}
