package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
import com.tissue.feature.project.application.dto.response.ProjectMemberStats;
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
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectMemberStatsIntegrationTest extends IntegrationTestSupport {

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
    private Member memberB;
    private Member memberC;
    private ProjectMember assigneeA;
    private ProjectMember assigneeB;
    private Project project;

    private WorkflowState todo;
    private WorkflowState active;
    private WorkflowState done;
    private WorkflowState cancelled;
    private IssueType standardType;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("a@tissue.com", "usera", "User A"));
        memberB = memberRepository.save(Member.create("b@tissue.com", "userb", "User B"));
        memberC = memberRepository.save(Member.create("c@tissue.com", "userc", "User C"));
        project = projectRepository.save(Project.create("PROJ", "Proj", null));
        assigneeA = projectMemberRepository.save(ProjectMember.create(project, actor));
        assigneeB = projectMemberRepository.save(ProjectMember.create(project, memberB));
        projectMemberRepository.save(ProjectMember.create(project, memberC)); // a member with no assigned issues

        Workflow workflow = Workflow.create(Name.of("Stats WF"), null, ColorType.ANSI_YELLOW);
        todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        active = workflow.addState(Name.of("DOING"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        cancelled = workflow.addState(Name.of("CANCELLED"), null, ColorType.ANSI_RED, StateCategory.ABORTED);
        workflowRepository.save(workflow);
        standardType = issueTypeRepository.save(IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow));

        em.flush();
    }

    @Test
    @DisplayName("aggregates resolved/open counts, resolved story points and completion rate per member")
    void aggregatesPerMember() {
        // given - A: 2 resolved (SP 3+5), 1 active + 1 todo open; B: 1 resolved (SP 2, no open)
        saveIssue(assigneeA, done, 3);
        saveIssue(assigneeA, done, 5);
        saveIssue(assigneeA, active, null);
        saveIssue(assigneeA, todo, null);
        saveIssue(assigneeB, done, 2);

        // when
        List<ProjectMemberStats> stats = statsOf("PROJ");

        // then
        ProjectMemberStats a = rowFor(stats, actor.getId());
        assertThat(a.resolvedCount()).isEqualTo(2);
        assertThat(a.openAssignedCount()).isEqualTo(2);
        assertThat(a.totalStoryPoints()).isEqualTo(8);
        assertThat(a.completionRate()).isEqualTo(0.5);

        ProjectMemberStats b = rowFor(stats, memberB.getId());
        assertThat(b.resolvedCount()).isEqualTo(1);
        assertThat(b.openAssignedCount()).isZero();
        assertThat(b.totalStoryPoints()).isEqualTo(2);
        assertThat(b.completionRate()).isEqualTo(1.0);

        // both members have resolved issues, so both carry an average resolve time
        assertThat(a.avgResolveSeconds()).isNotNull().isGreaterThanOrEqualTo(0L);
        assertThat(b.avgResolveSeconds()).isNotNull().isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("avgResolveSeconds is the created-to-resolved gap, and null when the member has no resolved issues")
    void avgResolveSecondsReflectsGap() {
        // given - A resolved an issue that was created two hours before it was resolved; B has only an open one
        Issue resolved = saveIssue(assigneeA, done, null);
        backdateCreatedAt(resolved, Instant.now().minus(2, ChronoUnit.HOURS));
        saveIssue(assigneeB, active, null);

        // when
        List<ProjectMemberStats> stats = statsOf("PROJ");

        // then
        assertThat(rowFor(stats, actor.getId()).avgResolveSeconds()).isCloseTo(7200L, within(120L));
        assertThat(rowFor(stats, memberB.getId()).avgResolveSeconds()).isNull();
    }

    @Test
    @DisplayName("excludes ABORTED, unassigned and soft-deleted issues; omits members with no assigned issues")
    void excludesNonCountingIssues() {
        // given
        saveIssue(assigneeA, done, 4); // A: 1 resolved
        saveIssue(assigneeA, cancelled, 9); // ABORTED: excluded from both counts and story points
        saveIssue(null, done, 7); // unassigned: no member row
        Issue deleted = saveIssue(assigneeA, active, null);
        deleted.softDelete();
        issueCommandRepository.save(deleted);
        em.flush();

        // when
        List<ProjectMemberStats> stats = statsOf("PROJ");

        // then
        ProjectMemberStats a = rowFor(stats, actor.getId());
        assertThat(a.resolvedCount()).isEqualTo(1);
        assertThat(a.openAssignedCount()).isZero(); // the ACTIVE one was soft-deleted
        assertThat(a.totalStoryPoints()).isEqualTo(4); // the ABORTED issue's 9 points are not counted
        assertThat(a.completionRate()).isEqualTo(1.0);

        // members with no assigned issues (B, C) and the unassigned issue produce no rows
        assertThat(stats).extracting(ProjectMemberStats::memberId).containsExactly(actor.getId());
    }

    @Test
    @DisplayName("returns no rows for a project with no assigned issues")
    void returnsNoRowsWhenNoAssignedIssues() {
        saveIssue(null, done, 1); // only an unassigned issue

        assertThat(statsOf("PROJ")).isEmpty();
    }

    private Issue saveIssue(@Nullable ProjectMember assignee, WorkflowState targetState, @Nullable Integer storyPoint) {
        Issue issue = Issue.create(
                project,
                null,
                standardType,
                "issue",
                IssueContent.of("c", "s"),
                IssueSchedule.of(null),
                IssueParticipants.of(assignee),
                IssuePriority.P2,
                storyPoint,
                null);
        if (!targetState.getCategory().isInitial()) {
            issue.transitionTo(targetState);
        }
        Issue saved = issueCommandRepository.save(issue);
        em.flush();
        return saved;
    }

    private void backdateCreatedAt(Issue issue, Instant past) {
        em.createNativeQuery("UPDATE issue SET created_at = :createdAt WHERE id = :id")
                .setParameter("createdAt", past)
                .setParameter("id", issue.getId())
                .executeUpdate();
        em.flush();
        em.clear();
    }

    private List<ProjectMemberStats> statsOf(String key) {
        return sut.getProjectMemberStats(ProjectIdentifier.ofProjectKey(key), actor.getId());
    }

    private ProjectMemberStats rowFor(List<ProjectMemberStats> stats, Long memberId) {
        return stats.stream()
                .filter(s -> s.memberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no stats row for member " + memberId));
    }
}
