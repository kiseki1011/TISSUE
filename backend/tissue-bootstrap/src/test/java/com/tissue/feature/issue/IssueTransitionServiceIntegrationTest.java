package com.tissue.feature.issue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueLifecycleService;
import com.tissue.feature.issue.application.service.IssueTransitionService;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.feature.issue.domain.exception.TransitionSourceStateMismatchException;
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
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.vo.Name;
import com.tissue.support.IntegrationTestSupport;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class IssueTransitionServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private IssueTransitionService issueTransitionService;

    @Autowired
    private IssueLifecycleService issueLifecycleService;

    @Autowired
    private IssueQueryRepository issueQueryRepository;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    private static final ProjectIdentifier PID = ProjectIdentifier.ofProjectKey("PROJ");

    private Member member;
    private Long issueTypeId;
    private Long startTransitionId;
    private Long completeTransitionId;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "HongGilDong"));

        Project project = projectRepository.save(Project.create("PROJ", "Test Project", null));
        projectMemberRepository.save(ProjectMember.createManager(project, member));

        Workflow workflow = Workflow.create(Name.of("Test Workflow"), null, ColorType.ANSI_YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.ANSI_GREEN, StateCategory.INITIAL);
        WorkflowState inProgress =
                workflow.addState(Name.of("IN PROGRESS"), null, ColorType.ANSI_BLUE, StateCategory.ACTIVE);
        WorkflowState done = workflow.addState(Name.of("DONE"), null, ColorType.ANSI_BLACK, StateCategory.COMPLETED);
        workflow.addTransition(Name.of("Start"), null, todo, inProgress);
        workflow.addTransition(Name.of("Complete"), null, inProgress, done);

        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                Name.of("Story"), null, ColorType.ANSI_RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
        issueTypeRepository.save(issueType);
        issueTypeId = issueType.getId();

        em.flush();

        startTransitionId = workflow.getTransitions().stream()
                .filter(t -> t.getDisplayName().equals("Start"))
                .findFirst()
                .map(WorkflowTransition::getId)
                .orElseThrow();
        completeTransitionId = workflow.getTransitions().stream()
                .filter(t -> t.getDisplayName().equals("Complete"))
                .findFirst()
                .map(WorkflowTransition::getId)
                .orElseThrow();

        em.clear();
    }

    @Test
    @DisplayName("transitions issue from 'INITIAL' to 'ACTIVE' state and marks schedule started")
    void successTransitionToActive() {
        // given
        String issueKey = createBasicIssue();
        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);

        // when
        issueTransitionService.performTransition(iid, startTransitionId, member.getId());
        em.flush();
        em.clear();

        // then
        Issue issue = issueQueryRepository.findWithBasicInfoByKey(issueKey).orElseThrow();
        assertThat(issue.getCurrentState().getCategory()).isEqualTo(StateCategory.ACTIVE);
        assertThat(issue.getSchedule().getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("transitions issue to 'COMPLETED' state and marks schedule resolved")
    void successTransitionToCompleted() {
        // given
        String issueKey = createBasicIssue();
        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);
        issueTransitionService.performTransition(iid, startTransitionId, member.getId());
        em.flush();
        em.clear();

        // when
        issueTransitionService.performTransition(iid, completeTransitionId, member.getId());
        em.flush();
        em.clear();

        // then
        Issue issue = issueQueryRepository.findWithBasicInfoByKey(issueKey).orElseThrow();
        assertThat(issue.getCurrentState().getCategory()).isEqualTo(StateCategory.COMPLETED);
        assertThat(issue.getSchedule().getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("fails if transition source state does not match issue's current state")
    void failTransition_If_SourceStateMismatch() {
        // given
        String issueKey = createBasicIssue();
        IssueIdentifier iid = IssueIdentifier.ofIssueKey(issueKey);

        // when & then
        assertThatThrownBy(() -> issueTransitionService.performTransition(iid, completeTransitionId, member.getId()))
                .isInstanceOf(TransitionSourceStateMismatchException.class);
    }

    private String createBasicIssue() {
        CreateIssueCommand cmd = CreateIssueCommand.builder()
                .title("Test Issue")
                .priority(IssuePriority.P2)
                .issueTypeId(issueTypeId)
                .customFields(Map.of())
                .build();

        IssueCreateResponse response = issueLifecycleService.create(PID, cmd, member.getId());
        em.flush();
        em.clear();
        return response.issueKey();
    }
}
