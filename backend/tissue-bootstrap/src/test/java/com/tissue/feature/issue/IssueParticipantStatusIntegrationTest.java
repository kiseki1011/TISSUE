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
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * A LOCKED/DELETED/PURGED member keeps its {@code ProjectMember} row (for attribution) and can still
 * be referenced by id, so assignment/review must be refused for anyone but an ACTIVE member.
 */
@LLMGenerated(llmInvolvement = LLMInvolvement.ASSISTED, evaluation = Evaluation.NOT_REVIEWED, model = "claude-opus-4-8")
@Transactional
class IssueParticipantStatusIntegrationTest extends IntegrationTestSupport {

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
    private Member target;
    private Long issueTypeId;

    @BeforeEach
    void setUp() {
        actor = memberRepository.save(Member.create("actor@tissue.com", "actor", "Actor"));
        target = memberRepository.save(Member.create("target@tissue.com", "target", "Target"));

        Project proj = projectRepository.save(Project.create("PROJ", "Proj", null));
        projectMemberRepository.save(ProjectMember.createManager(proj, actor));
        projectMemberRepository.save(ProjectMember.create(proj, target));

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
    }

    private String createIssue(Long assigneeMemberId) {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .title("t")
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

    private void lockTarget() {
        em.find(Member.class, target.getId()).lock();
        em.flush();
        em.clear();
    }

    private void withdrawTarget() {
        em.find(Member.class, target.getId()).withdraw();
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("assign to an ACTIVE member succeeds (the guard does not break the normal path)")
    void assign_toActiveMember_succeeds() {
        // given
        String issueKey = createIssue(null);

        // when
        sut.assign(IssueIdentifier.ofIssueKey(issueKey), target.getId(), actor.getId());
        em.flush();
        em.clear();

        // then
        Issue issue = issueFinder.getWithProjectByIssueKey(issueKey);
        assertThat(issue.getParticipants().getAssignee().getMemberId()).isEqualTo(target.getId());
    }

    @Test
    @DisplayName("assign to a LOCKED member is rejected")
    void assign_toLockedMember_isRejected() {
        // given
        String issueKey = createIssue(null);
        lockTarget();

        // when & then
        assertThatThrownBy(() -> sut.assign(IssueIdentifier.ofIssueKey(issueKey), target.getId(), actor.getId()))
                .isInstanceOfSatisfying(BadRequestException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(IssueErrorCode.CANNOT_ASSIGN_INACTIVE_MEMBER));
    }

    @Test
    @DisplayName("assign to a DELETED member is rejected")
    void assign_toDeletedMember_isRejected() {
        // given
        String issueKey = createIssue(null);
        withdrawTarget();

        // when & then
        assertThatThrownBy(() -> sut.assign(IssueIdentifier.ofIssueKey(issueKey), target.getId(), actor.getId()))
                .isInstanceOfSatisfying(BadRequestException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(IssueErrorCode.CANNOT_ASSIGN_INACTIVE_MEMBER));
    }

    @Test
    @DisplayName("adding a LOCKED member as reviewer is rejected")
    void addReviewer_forLockedMember_isRejected() {
        // given
        String issueKey = createIssue(null);
        lockTarget();

        // when & then
        assertThatThrownBy(() -> sut.addReviewer(IssueIdentifier.ofIssueKey(issueKey), target.getId(), actor.getId()))
                .isInstanceOfSatisfying(BadRequestException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(IssueErrorCode.CANNOT_ADD_INACTIVE_REVIEWER));
    }

    @Test
    @DisplayName("creating an issue with a DELETED initial assignee is rejected")
    void create_withInactiveInitialAssignee_isRejected() {
        // given
        withdrawTarget();

        // when & then
        assertThatThrownBy(() -> createIssue(target.getId()))
                .isInstanceOfSatisfying(BadRequestException.class, ex -> assertThat(ex.getErrorCode())
                        .isEqualTo(IssueErrorCode.CANNOT_ASSIGN_INACTIVE_MEMBER));
    }
}
