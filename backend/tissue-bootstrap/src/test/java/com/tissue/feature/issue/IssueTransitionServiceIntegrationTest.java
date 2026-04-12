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
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
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
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    private static final ProjectIdentifier PID = new ProjectIdentifier("WORKSPACE", "PROJ");

    private Member member;
    private Long issueTypeId;
    private Long startTransitionId;
    private Long completeTransitionId;

    @BeforeEach
    void setUp() {
        member = memberRepository.save(Member.create("test@tissue.com", "testuser", "HongGilDong"));

        Workspace workspace = workspaceRepository.save(Workspace.create(PID.workspaceKey(), "Test Workspace", null));
        Project project = projectRepository.save(Project.create(workspace, PID.projectKey(), "Test Project", null));
        WorkspaceMember workspaceMember =
                workspaceMemberRepository.save(WorkspaceMember.create(member, workspace, WorkspaceRole.OWNER));
        projectMemberRepository.save(ProjectMember.createManager(project, workspaceMember));

        Workflow workflow = Workflow.create(project, Name.of("Test Workflow"), null, ColorType.YELLOW);
        WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.GREEN, StateCategory.INITIAL);
        WorkflowState inProgress =
                workflow.addState(Name.of("IN PROGRESS"), null, ColorType.BLUE, StateCategory.ACTIVE);
        WorkflowState done = workflow.addState(Name.of("DONE"), null, ColorType.BLACK, StateCategory.COMPLETED);
        workflow.addTransition(Name.of("Start"), null, todo, inProgress);
        workflow.addTransition(Name.of("Complete"), null, inProgress, done);

        workflowRepository.save(workflow);

        IssueType issueType = IssueType.create(
                project,
                Name.of("Story"),
                null,
                ColorType.RED,
                IconType.CIRCLE_FILLED,
                IssueHierarchy.STANDARD,
                workflow);
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
        IssueIdentifier iid = new IssueIdentifier(PID.workspaceKey(), PID.projectKey(), issueKey);

        // when
        issueTransitionService.performTransition(iid, startTransitionId, member.getId());
        em.flush();
        em.clear();

        // then
        Issue issue = issueQueryRepository
                .findWithBasicInfo(PID.workspaceKey(), issueKey)
                .orElseThrow();
        assertThat(issue.getCurrentState().getCategory()).isEqualTo(StateCategory.ACTIVE);
        assertThat(issue.getSchedule().getStartedAt()).isNotNull();
    }

    @Test
    @DisplayName("transitions issue to 'COMPLETED' state and marks schedule resolved")
    void successTransitionToCompleted() {
        // given
        String issueKey = createBasicIssue();
        IssueIdentifier iid = new IssueIdentifier(PID.workspaceKey(), PID.projectKey(), issueKey);
        issueTransitionService.performTransition(iid, startTransitionId, member.getId());
        em.flush();
        em.clear();

        // when
        issueTransitionService.performTransition(iid, completeTransitionId, member.getId());
        em.flush();
        em.clear();

        // then
        Issue issue = issueQueryRepository
                .findWithBasicInfo(PID.workspaceKey(), issueKey)
                .orElseThrow();
        assertThat(issue.getCurrentState().getCategory()).isEqualTo(StateCategory.COMPLETED);
        assertThat(issue.getSchedule().getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("fails if transition source state does not match issue's current state")
    void failTransition_If_SourceStateMismatch() {
        // given
        String issueKey = createBasicIssue();
        IssueIdentifier iid = new IssueIdentifier(PID.workspaceKey(), PID.projectKey(), issueKey);

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
