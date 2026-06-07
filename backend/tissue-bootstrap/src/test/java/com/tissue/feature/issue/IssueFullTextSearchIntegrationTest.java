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
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Sql(scripts = "/db/fts.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
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

        Workflow workflow = Workflow.create(Name.of("Default"), null, ColorType.YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.GREEN, StateCategory.INITIAL);
        WorkflowState inProgress =
                workflow.addState(Name.of("IN PROGRESS"), null, ColorType.BLUE, StateCategory.ACTIVE);
        workflow.addTransition(Name.of("Start"), null, todo, inProgress);
        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                Name.of("Story"), null, ColorType.RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
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
            CursorPage<IssueSummary> page = search("deployment");

            // then
            assertThat(page.content()).extracting(IssueSummary::issueKey).containsExactly(match);
        }

        @Test
        @DisplayName("success: matches a whole word in the content")
        void matchesContentWord() {
            // given
            String match = createIssue("Title A", "the rollback procedure", actor.getId());
            createIssue("Title B", "unrelated text", actor.getId());

            // when
            CursorPage<IssueSummary> page = search("rollback");

            // then
            assertThat(page.content()).extracting(IssueSummary::issueKey).containsExactly(match);
        }

        @Test
        @DisplayName("success: matching is case-insensitive")
        void matchesCaseInsensitively() {
            // given
            String match = createIssue("Deployment guide", "body", actor.getId());

            // when
            CursorPage<IssueSummary> page = search("DEPLOYMENT");

            // then
            assertThat(page.content()).extracting(IssueSummary::issueKey).containsExactly(match);
        }

        @Test
        @DisplayName("behavior: 'simple' config is word-based — a prefix does NOT match (no stemming/substring)")
        void prefixDoesNotMatch() {
            // given
            createIssue("Deployment guide", "body", actor.getId());

            // when
            CursorPage<IssueSummary> page = search("deploy");

            // then
            assertThat(page.content()).isEmpty();
        }

        @Test
        @DisplayName("success: returns empty when nothing matches")
        void noMatch() {
            // given
            createIssue("Deployment guide", "body", actor.getId());

            // when & then
            assertThat(search("nonexistent").content()).isEmpty();
        }

        @Test
        @DisplayName("success: blank keyword returns empty")
        void blankKeyword() {
            // given
            createIssue("Deployment guide", "body", actor.getId());

            // when & then
            assertThat(search(" ").content()).isEmpty();
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
            CursorPage<IssueSummary> page = sut.ftsByProjectKeyset(PROJ, condition, null, 20, actor.getId());

            // then
            assertThat(page.content()).extracting(IssueSummary::issueKey).containsExactly(mine);
        }
    }

    private CursorPage<IssueSummary> search(String keyword) {
        IssueSearchCondition condition = new IssueSearchCondition(
                null, null, null, null, null, null, null, null, null, null, null, null, keyword);
        return sut.ftsByProjectKeyset(PROJ, condition, null, 20, actor.getId());
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
