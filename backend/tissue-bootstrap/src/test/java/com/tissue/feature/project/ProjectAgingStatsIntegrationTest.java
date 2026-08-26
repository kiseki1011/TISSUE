package com.tissue.feature.project;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.issue.application.port.repository.IssueCommandRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueContent;
import com.tissue.feature.issue.domain.IssueParticipants;
import com.tissue.feature.issue.domain.IssueSchedule;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.enums.IssueRelationType;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectAgingStats;
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
class ProjectAgingStatsIntegrationTest extends IntegrationTestSupport {

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

    private WorkflowState todo;
    private WorkflowState done;

    private IssueType standardType;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        project = projectRepository.save(Project.create("PROJ", "Proj", null));
        projectMemberRepository.save(ProjectMember.create(project, actor));

        Workflow workflow = Workflow.create(Name.of("Aging WF"), null, ColorType.ANSI_YELLOW);
        todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        workflow.addState(Name.of("DOING"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflowRepository.save(workflow);

        standardType = issueTypeRepository.save(IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow));

        em.flush();
    }

    @Test
    @DisplayName("buckets open issues by age, measured from creation when not yet started")
    void bucketsOpenIssuesByAge() {
        // given
        Instant now = Instant.now();
        backdateAge(saveIssue(todo).getId(), now.minus(1, ChronoUnit.DAYS), null);
        backdateAge(saveIssue(todo).getId(), now.minus(5, ChronoUnit.DAYS), null);
        backdateAge(saveIssue(todo).getId(), now.minus(10, ChronoUnit.DAYS), null);
        backdateAge(saveIssue(todo).getId(), now.minus(20, ChronoUnit.DAYS), null);

        // when
        ProjectAgingStats stats = agingOf("PROJ");

        // then
        assertThat(stats.agingUnder3d()).isEqualTo(1);
        assertThat(stats.aging3to7d()).isEqualTo(1);
        assertThat(stats.aging1to2w()).isEqualTo(1);
        assertThat(stats.agingOver2w()).isEqualTo(1);
        assertThat(stats.openTotal()).isEqualTo(4);
    }

    @Test
    @DisplayName("measures age from startedAt when the issue has started, not from creation")
    void measuresAgeFromStartedAtWhenPresent() {
        // given - created recently but started long ago: COALESCE must pick startedAt
        Instant now = Instant.now();
        backdateAge(saveIssue(todo).getId(), now.minus(1, ChronoUnit.DAYS), now.minus(20, ChronoUnit.DAYS));

        // when
        ProjectAgingStats stats = agingOf("PROJ");

        // then
        assertThat(stats.agingOver2w()).isEqualTo(1);
        assertThat(stats.agingUnder3d()).isZero();
        assertThat(stats.openTotal()).isEqualTo(1);
    }

    @Test
    @DisplayName("excludes completed issues from the aging buckets")
    void excludesCompletedIssues() {
        // given
        Instant now = Instant.now();
        backdateAge(saveIssue(done).getId(), now.minus(20, ChronoUnit.DAYS), null);

        // when
        ProjectAgingStats stats = agingOf("PROJ");

        // then
        assertThat(stats.openTotal()).isZero();
        assertThat(stats.agingOver2w()).isZero();
    }

    @Test
    @DisplayName("counts open issues blocked by another still-open issue")
    void countsBlockedOpenIssues() {
        // given
        Issue blocker = saveIssue(todo);
        Issue blocked = saveIssue(todo);
        em.persist(blocker.addRelation(blocked, IssueRelationType.BLOCKS));
        em.flush();
        em.clear();

        // when
        ProjectAgingStats stats = agingOf("PROJ");

        // then
        assertThat(stats.blocked()).isEqualTo(1);
    }

    @Test
    @DisplayName("does not count an issue as blocked once its blocker is completed")
    void ignoresBlockedWhenBlockerCompleted() {
        // given
        Issue blocker = saveIssue(done);
        Issue blocked = saveIssue(todo);
        em.persist(blocker.addRelation(blocked, IssueRelationType.BLOCKS));
        em.flush();
        em.clear();

        // when
        ProjectAgingStats stats = agingOf("PROJ");

        // then
        assertThat(stats.blocked()).isZero();
    }

    @Test
    @DisplayName("returns zeroed aging stats for a project with no issues")
    void returnsZeroedStatsForEmptyProject() {
        // when
        ProjectAgingStats stats = agingOf("PROJ");

        // then
        assertThat(stats.openTotal()).isZero();
        assertThat(stats.agingUnder3d()).isZero();
        assertThat(stats.aging3to7d()).isZero();
        assertThat(stats.aging1to2w()).isZero();
        assertThat(stats.agingOver2w()).isZero();
        assertThat(stats.blocked()).isZero();
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

    private void backdateAge(Long issueId, Instant createdAt, @Nullable Instant startedAt) {
        em.createNativeQuery("UPDATE issue SET created_at = :ts WHERE id = :id")
                .setParameter("ts", createdAt)
                .setParameter("id", issueId)
                .executeUpdate();
        if (startedAt != null) {
            em.createNativeQuery("UPDATE issue SET started_at = :ts WHERE id = :id")
                    .setParameter("ts", startedAt)
                    .setParameter("id", issueId)
                    .executeUpdate();
        }
        em.flush();
        em.clear();
    }

    private ProjectAgingStats agingOf(String key) {
        return sut.getProjectAgingStats(ProjectIdentifier.ofProjectKey(key), actor.getId());
    }
}
