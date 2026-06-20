package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.service.IssueFullTextSearchService;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        model = "claude-opus-4-8",
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Reviewed.",
        reviewedBy = "kiseki1011")
@Transactional
class IssueFullTextSearchIntegrationTest extends IntegrationTestSupport {

    private static final ProjectIdentifier PROJ = ProjectIdentifier.ofProjectKey("PROJ");

    @Autowired
    private IssueFullTextSearchService sut;

    @Autowired
    private IssueLifecycleService issueLifecycleService;

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

    private Member actor;
    private Member other;
    private Long issueTypeId;
    private Long fieldId;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        other = memberRepository.save(Member.create("other@tissue.com", "other", "Other"));

        Project proj = projectRepository.save(Project.create("PROJ", "Proj", null));
        projectMemberRepository.save(ProjectMember.createManager(proj, actor));
        projectMemberRepository.save(ProjectMember.create(proj, other));

        Workflow workflow = Workflow.create(Name.of("Default"), null, ColorType.ANSI_YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        WorkflowState inProgress =
                workflow.addState(Name.of("IN PROGRESS"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        workflow.addTransition(Name.of("Start"), null, todo, inProgress);
        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
        issueTypeRepository.save(issueType);
        IssueField goalField = issueType.addField(Name.of("goal"), "Goal", IssueFieldType.TEXT, true, 0);

        em.flush();
        issueTypeId = issueType.getId();
        fieldId = goalField.getId();
        em.clear();

        setSecurityContext(actor);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(Member member) {
        MemberDetails details = new MemberDetails(member.getId(), member.getEmail(), member.getUsername(), List.of());
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("keyword matching")
    class KeywordMatching {

        @Test
        @DisplayName("success: matches a whole word in the title")
        void matchesTitleWord() {
            // given
            String match = createIssue("Deployment guide", "body", actor.getId());
            createIssue("Random note", "body", actor.getId());

            // when
            Page<IssueSummary> page = search("deployment");

            // then
            assertThat(page.getContent()).extracting(IssueSummary::issueKey).containsExactly(match);
        }

        @Test
        @DisplayName("success: matches a whole word in the content")
        void matchesContentWord() {
            // given
            String match = createIssue("Title A", "the rollback procedure", actor.getId());
            createIssue("Title B", "unrelated text", actor.getId());

            // when
            Page<IssueSummary> page = search("rollback");

            // then
            assertThat(page.getContent()).extracting(IssueSummary::issueKey).containsExactly(match);
        }

        @Test
        @DisplayName("success: matching is case-insensitive")
        void matchesCaseInsensitively() {
            // given
            String match = createIssue("Deployment guide", "body", actor.getId());

            // when
            Page<IssueSummary> page = search("DEPLOYMENT");

            // then
            assertThat(page.getContent()).extracting(IssueSummary::issueKey).containsExactly(match);
        }

        @Test
        @DisplayName("success: a word prefix matches (to_tsquery ':*' prefix search)")
        void prefixMatches() {
            // given
            String match = createIssue("Deployment guide", "body", actor.getId());

            // when - 'deploy' is a prefix of 'deployment'
            Page<IssueSummary> page = search("deploy");

            // then
            assertThat(page.getContent()).extracting(IssueSummary::issueKey).containsExactly(match);
        }

        @Test
        @DisplayName("behavior: only word prefixes match, not an arbitrary infix")
        void infixDoesNotMatch() {
            // given
            createIssue("Deployment guide", "body", actor.getId());

            // when & then - 'ployment' sits inside 'deployment' but is not a prefix
            assertThat(search("ployment").getContent()).isEmpty();
        }

        @Test
        @DisplayName("success: each space-separated prefix term must match (AND)")
        void multiTermPrefixesAreAnded() {
            // given
            String match = createIssue("Deployment guide", "body", actor.getId());
            createIssue("Deployment notes", "body", actor.getId());

            // when - both 'depl*' and 'gui*' must be present
            Page<IssueSummary> page = search("depl gui");

            // then
            assertThat(page.getContent()).extracting(IssueSummary::issueKey).containsExactly(match);
        }

        @Test
        @DisplayName("success: returns empty when nothing matches")
        void noMatch() {
            // given
            createIssue("Deployment guide", "body", actor.getId());

            // when & then
            assertThat(search("nonexistent").getContent()).isEmpty();
        }

        @Test
        @DisplayName("success: blank keyword returns empty")
        void blankKeyword() {
            // given
            createIssue("Deployment guide", "body", actor.getId());

            // when & then
            assertThat(search(" ").getContent()).isEmpty();
        }
    }

    @LLMGenerated(
            llmInvolvement = LLMInvolvement.VIBE_CODED,
            model = "claude-opus-4-8",
            evaluation = Evaluation.ACCEPTABLE,
            evaluationReason = "Reviewed, but needs more case testing.",
            reviewedBy = "kiseki1011")
    @Nested
    @DisplayName("relevance ordering")
    class RelevanceOrdering {

        // TODO: Add more tests
        //  - multiple keyword search + priority
        //  - partial keyword search + frequency
        //  - partial keyword search + priority

        @Test
        @DisplayName("success: higher term frequency ranks first, beating recency")
        void ranksByRelevanceOverRecency() {
            // given
            // 'more' has 4 occurrences of the lexeme but is created FIRST (older, lower id);
            // 'less' has 1 occurrence but is created LAST (newer, higher id). Recency alone would put
            // 'less' first, so a 'more'-first result proves relevance (ts_rank) drives the order.
            String more = createIssue("rollback rollback", "rollback rollback", actor.getId());
            String less = createIssue("Note", "rollback", actor.getId());

            // when
            Page<IssueSummary> page = search("rollback");

            // then
            assertThat(page.getContent()).extracting(IssueSummary::issueKey).containsExactly(more, less);
        }
    }

    @Nested
    @DisplayName("keyword combined with filters")
    class KeywordWithFilters {

        @Test
        @DisplayName("success: keyword AND assignee filter")
        void keywordAndAssignee() {
            // given - same keyword, different assignees
            String mine = createIssue("Deployment guide", "body", actor.getId());
            createIssue("Deployment runbook", "body", other.getId());

            IssueSearchCondition condition = new IssueSearchCondition(
                    null,
                    null,
                    null,
                    null,
                    null,
                    Set.of(actor.getId()),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    "deployment");

            // when
            Page<IssueSummary> page = sut.ftsByProjectRanked(PROJ, condition, 0, 20, actor.getId());

            // then
            assertThat(page.getContent()).extracting(IssueSummary::issueKey).containsExactly(mine);
        }
    }

    @Nested
    @DisplayName("instance-wide search")
    class InstanceWideSearch {

        @Test
        @DisplayName("success: scopes results to the caller's project memberships")
        void scopedToMemberships() {
            // given - actor is a member of PROJ (setUp); PROJ2 has only 'other' as a member
            createProject("PROJ2", other);
            String mine = createIssue("deployment guide", "body", actor.getId());
            String theirs = createIssueIn(ProjectIdentifier.ofProjectKey("PROJ2"), "deployment runbook", "body", other);

            // when
            Page<IssueSummary> page = sut.ftsAllRanked(condition("deployment"), 0, 20, actor.getId());

            // then - only the issue in the project the actor belongs to
            assertThat(page.getContent()).extracting(IssueSummary::issueKey).containsExactly(mine);
            assertThat(page.getContent()).extracting(IssueSummary::issueKey).doesNotContain(theirs);
        }

        @Test
        @DisplayName("success: spans every project the caller is a member of")
        void spansMemberProjects() {
            // given - actor is a member of both PROJ and PROJ2
            createProject("PROJ2", actor);
            String a = createIssue("rollback plan", "body", actor.getId());
            String b = createIssueIn(ProjectIdentifier.ofProjectKey("PROJ2"), "rollback steps", "body", actor);

            // when
            Page<IssueSummary> page = sut.ftsAllRanked(condition("rollback"), 0, 20, actor.getId());

            // then
            assertThat(page.getContent()).extracting(IssueSummary::issueKey).containsExactlyInAnyOrder(a, b);
        }
    }

    @LLMGenerated(
            llmInvolvement = LLMInvolvement.ASSISTED,
            evaluation = Evaluation.NOT_REVIEWED,
            evaluationReason = "Needs review.",
            model = "claude-opus-4-8")
    @Nested
    @DisplayName("empty results stay paged (no unpaged 'INSTANCE' envelope)")
    class EmptyPagesArePaged {

        @Test
        @DisplayName("success: blank keyword returns a paged empty page, not an unpaged one")
        void blankKeywordIsPaged() {
            // given
            createIssue("Deployment guide", "body", actor.getId());

            // when
            Page<IssueSummary> page = sut.ftsByProjectRanked(PROJ, condition(" "), 0, 20, actor.getId());

            // then
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getPageable().isPaged()).isTrue();
            assertThat(page.getPageable()).isEqualTo(PageRequest.of(0, 20));
        }

        @Test
        @DisplayName("success: a caller with no project memberships returns a paged empty page")
        void noMembershipsIsPaged() {
            // given - a member who belongs to no project
            Member loner = memberRepository.save(Member.create("loner@tissue.com", "loner", "Loner"));

            // when
            Page<IssueSummary> page = sut.ftsAllRanked(condition("deployment"), 0, 20, loner.getId());

            // then
            assertThat(page.getContent()).isEmpty();
            assertThat(page.getPageable().isPaged()).isTrue();
            assertThat(page.getPageable()).isEqualTo(PageRequest.of(0, 20));
        }
    }

    private Page<IssueSummary> search(String keyword) {
        return sut.ftsByProjectRanked(PROJ, condition(keyword), 0, 20, actor.getId());
    }

    private IssueSearchCondition condition(String keyword) {
        return new IssueSearchCondition(
                null, null, null, null, null, null, null, null, null, null, null, null, keyword);
    }

    private void createProject(String key, Member manager) {
        Project project = projectRepository.save(Project.create(key, key, null));
        projectMemberRepository.save(ProjectMember.createManager(project, manager));
        em.flush();
        em.clear();
    }

    private String createIssueIn(ProjectIdentifier pid, String title, String content, Member author) {
        setSecurityContext(author);
        try {
            CreateIssueCommand cmd = CreateIssueCommand.builder()
                    .sprintId(null)
                    .parentProjectKey(null)
                    .parentKey(null)
                    .title(title)
                    .content(content)
                    .summary("s")
                    .priority(IssuePriority.P3)
                    .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                    .storyPoint(3)
                    .issueTypeId(issueTypeId)
                    .customFields(Map.of(fieldId, "v"))
                    .assigneeMemberId(author.getId())
                    .build();
            String issueKey =
                    issueLifecycleService.create(pid, cmd, author.getId()).issueKey();
            em.flush();
            em.clear();
            return issueKey;
        } finally {
            setSecurityContext(actor);
        }
    }

    private String createIssue(String title, String content, Long assigneeMemberId) {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .sprintId(null)
                .parentProjectKey(null)
                .parentKey(null)
                .title(title)
                .content(content)
                .summary("s")
                .priority(IssuePriority.P3)
                .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .storyPoint(3)
                .issueTypeId(issueTypeId)
                .customFields(Map.of(fieldId, "v"))
                .assigneeMemberId(assigneeMemberId)
                .build();
        String issueKey = issueLifecycleService.create(PROJ, cmd, actor.getId()).issueKey();
        em.flush();
        em.clear();
        return issueKey;
    }
}
