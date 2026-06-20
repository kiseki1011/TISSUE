package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.application.service.IssueParticipantService;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.exception.IssueErrorCode;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
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
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.ASSISTED,
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Mostly simple tests to validate claim logic",
        model = "claude-opus-4-8")
@Transactional
class IssueClaimIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueParticipantService sut;

    @Autowired
    private IssueLifecycleService issueLifecycleService;

    @Autowired
    private IssueFinder issueFinder;

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

    private static final ProjectIdentifier PROJ = ProjectIdentifier.ofProjectKey("PROJ");

    private Member actor;
    private Member other;
    private Long issueTypeId;

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

        em.flush();
        issueTypeId = issueType.getId();
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

    private String createIssue(String title, Long assigneeMemberId) {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .title(title)
                .content("c")
                .summary("s")
                .priority(IssuePriority.P3)
                .dueAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .storyPoint(3)
                .issueTypeId(issueTypeId)
                .customFields(Map.of())
                .assigneeMemberId(assigneeMemberId)
                .build();
        String issueKey = issueLifecycleService.create(PROJ, cmd, actor.getId()).issueKey();
        em.flush();
        em.clear();
        return issueKey;
    }

    @Test
    @DisplayName("claim assigns the issue to the actor when it is unassigned")
    void claim_whenUnassigned_assignsToActor() {
        // given
        String issueKey = createIssue("free", null);

        // when
        sut.claim(IssueIdentifier.ofIssueKey(issueKey), actor.getId());
        em.flush();
        em.clear();

        // then
        Issue issue = issueFinder.getWithProjectByIssueKey(issueKey);
        assertThat(issue.getParticipants().getAssignee().getMemberId()).isEqualTo(actor.getId());
    }

    @Test
    @DisplayName("claim throws a conflict when the issue is assigned to another member")
    void claim_whenAssignedToAnother_throwsConflict() {
        // given
        String issueKey = createIssue("taken", other.getId());

        // when & then
        assertThatThrownBy(() -> sut.claim(IssueIdentifier.ofIssueKey(issueKey), actor.getId()))
                .isInstanceOfSatisfying(ResourceConflictException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(IssueErrorCode.ISSUE_ALREADY_ASSIGNED));
    }

    @Test
    @DisplayName("claim is idempotent when the actor already holds the issue")
    void claim_byCurrentAssignee_idempotent() {
        // given
        String issueKey = createIssue("mine", actor.getId());

        // when
        sut.claim(IssueIdentifier.ofIssueKey(issueKey), actor.getId());
        em.flush();
        em.clear();

        // then
        Issue issue = issueFinder.getWithProjectByIssueKey(issueKey);
        assertThat(issue.getParticipants().getAssignee().getMemberId()).isEqualTo(actor.getId());
    }
}
