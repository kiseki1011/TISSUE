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
import com.tissue.feature.project.application.dto.response.ProjectContributionStats;
import com.tissue.feature.project.application.dto.response.ProjectContributionStats.ContributionDay;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.ProjectStatsQueryService;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
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
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectContributionStatsIntegrationTest extends IntegrationTestSupport {

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
    private ProjectMember actorPm;
    private ProjectMember otherPm;
    private Project project;

    private WorkflowState doing;
    private WorkflowState done;

    private IssueType standardType;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        Member other = memberRepository.save(Member.create("other@tissue.com", "other", "Other"));
        project = projectRepository.save(Project.create("PROJ", "Proj", null));
        actorPm = projectMemberRepository.save(ProjectMember.create(project, actor));
        otherPm = projectMemberRepository.save(ProjectMember.create(project, other));

        Workflow workflow = Workflow.create(Name.of("Flow WF"), null, ColorType.ANSI_YELLOW);
        workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        doing = workflow.addState(Name.of("DOING"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflowRepository.save(workflow);

        standardType = issueTypeRepository.save(IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow));
        em.flush();
    }

    @Test
    @DisplayName("buckets the member's resolved issues by day, zero-filled across the window")
    void bucketsResolvedByDay() {
        // given - the actor resolved 2 issues today and 1 two days ago
        Instant today = Instant.now();
        Instant twoDaysAgo = today.minus(2, ChronoUnit.DAYS);
        resolvedIssue(actorPm, today);
        resolvedIssue(actorPm, today);
        resolvedIssue(actorPm, twoDaysAgo);

        // when
        ProjectContributionStats stats = contributions(actor.getId(), 90);

        // then - dense zero-filled series over the whole window
        assertThat(stats.days()).hasSize(90);
        assertThat(stats.totalResolved()).isEqualTo(3);
        assertThat(stats.maxDaily()).isEqualTo(2);
        // the non-zero days are one with 2 and one with 1; all others are zero
        assertThat(stats.days()).extracting(ContributionDay::count).contains(2L, 1L);
        assertThat(stats.days().stream().mapToLong(ContributionDay::count).sum())
                .isEqualTo(3);
    }

    @Test
    @DisplayName("attributes by assignee: another member's resolved issues do not count")
    void attributesByAssignee() {
        // given - the actor resolved 1 issue; another member resolved 2
        resolvedIssue(actorPm, Instant.now());
        resolvedIssue(otherPm, Instant.now());
        resolvedIssue(otherPm, Instant.now());

        // when
        ProjectContributionStats stats = contributions(actor.getId(), 90);

        // then
        assertThat(stats.totalResolved()).isEqualTo(1);
    }

    @Test
    @DisplayName("counts only completed issues, not open ones assigned to the member")
    void countsOnlyCompleted() {
        // given - one resolved, one still in progress (both assigned to the actor)
        resolvedIssue(actorPm, Instant.now());
        Issue open = saveIssue(doing, actorPm);
        // an open issue has no resolvedAt, so it can never land in the heatmap regardless of the window
        assertThat(open.getSchedule().getResolvedAt()).isNull();

        // when
        ProjectContributionStats stats = contributions(actor.getId(), 90);

        // then
        assertThat(stats.totalResolved()).isEqualTo(1);
    }

    @Test
    @DisplayName("excludes resolutions older than the window")
    void excludesOldResolutions() {
        // given - one resolution inside a 30-day window, one 60 days ago (outside)
        resolvedIssue(actorPm, Instant.now());
        resolvedIssue(actorPm, Instant.now().minus(60, ChronoUnit.DAYS));

        // when
        ProjectContributionStats stats = contributions(actor.getId(), 30);

        // then
        assertThat(stats.days()).hasSize(30);
        assertThat(stats.totalResolved()).isEqualTo(1);
    }

    @Test
    @DisplayName("clamps an over-long window to a year and a non-positive one to a single day")
    void clampsWindow() {
        // when / then - a huge span is capped at 366 days
        assertThat(contributions(actor.getId(), 10_000).days()).hasSize(366);
        // and a zero/negative span collapses to today only
        assertThat(contributions(actor.getId(), 0).days()).hasSize(1);
    }

    @Test
    @DisplayName("returns an all-zero window when the member has resolved nothing")
    void emptyWhenNoResolutions() {
        // when
        ProjectContributionStats stats = contributions(actor.getId(), 90);

        // then
        assertThat(stats.days()).hasSize(90);
        assertThat(stats.totalResolved()).isZero();
        assertThat(stats.maxDaily()).isZero();
    }

    @Test
    @DisplayName("rejects a non-member: the heatmap is member-only")
    void rejectsNonMember() {
        // given
        Member outsider = memberRepository.save(Member.create("out@tissue.com", "outsider", "Outsider"));
        em.flush();

        // when / then
        assertThatThrownBy(() -> sut.getProjectContributions(
                        ProjectIdentifier.ofProjectKey("PROJ"), actor.getId(), 90, null, outsider.getId()))
                .isInstanceOf(ProjectMemberNotFoundException.class);
    }

    @Test
    @DisplayName("cuts the days on the requested zone, so a Seoul morning is not the previous day")
    void bucketsOnRequestedZone() {
        // given - resolved at 08:30 in Seoul, which is 23:30 the previous day in UTC
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        LocalDate seoulDay = LocalDate.now(seoul).minusDays(5);
        resolvedIssue(actorPm, seoulDay.atTime(8, 30).atZone(seoul).toInstant());

        // when
        ProjectContributionStats asSeoul = contributions(actor.getId(), 90, "Asia/Seoul");
        ProjectContributionStats asUtc = contributions(actor.getId(), 90, null);

        // then - the same instant lands on different days depending on the zone it is cut on
        assertThat(countOn(asSeoul, seoulDay)).isEqualTo(1);
        assertThat(countOn(asUtc, seoulDay)).isZero();
        assertThat(countOn(asUtc, seoulDay.minusDays(1))).isEqualTo(1);
    }

    @Test
    @DisplayName("an unrecognized zone falls back to UTC instead of failing the read")
    void unknownZoneFallsBackToUtc() {
        // given
        ZoneId seoul = ZoneId.of("Asia/Seoul");
        LocalDate seoulDay = LocalDate.now(seoul).minusDays(5);
        resolvedIssue(actorPm, seoulDay.atTime(8, 30).atZone(seoul).toInstant());

        // when
        ProjectContributionStats stats = contributions(actor.getId(), 90, "Not/AZone");

        // then - the panel still renders, on the UTC day
        assertThat(stats.totalResolved()).isEqualTo(1);
        assertThat(countOn(stats, seoulDay.minusDays(1))).isEqualTo(1);
    }

    private long countOn(ProjectContributionStats stats, LocalDate day) {
        return stats.days().stream()
                .filter(d -> d.date().equals(day))
                .mapToLong(ContributionDay::count)
                .sum();
    }

    private Issue saveIssue(WorkflowState state, ProjectMember assignee) {
        Issue issue = Issue.create(
                project,
                null,
                standardType,
                "issue",
                IssueContent.of("c", "s"),
                IssueSchedule.of(null),
                IssueParticipants.of(assignee),
                IssuePriority.P2,
                null,
                null);
        if (!state.getCategory().isInitial()) {
            issue.transitionTo(state);
        }
        Issue saved = issueCommandRepository.save(issue);
        em.flush();
        return saved;
    }

    private void resolvedIssue(ProjectMember assignee, Instant resolvedAt) {
        Issue issue = saveIssue(done, assignee);
        // transitionTo(done) stamps resolvedAt to now; override it to the day under test. Flush (not clear) so
        // the entities stay managed for later saveIssue calls; the service reads resolvedAt as a scalar, which
        // hits the DB and so sees this native update.
        em.createNativeQuery("UPDATE issue SET resolved_at = :ts WHERE id = :id")
                .setParameter("ts", resolvedAt)
                .setParameter("id", issue.getId())
                .executeUpdate();
        em.flush();
    }

    private ProjectContributionStats contributions(Long memberId, int days) {
        return contributions(memberId, days, null);
    }

    private ProjectContributionStats contributions(Long memberId, int days, @Nullable String zoneId) {
        return sut.getProjectContributions(
                ProjectIdentifier.ofProjectKey("PROJ"), memberId, days, zoneId, actor.getId());
    }
}
