package com.tissue.vcs.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueTransitionService;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.application.port.repository.WorkspaceVcsIntegrationRepository;
import com.tissue.feature.vcs.application.service.VcsIntegrationService;
import com.tissue.feature.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.exception.WorkspaceVcsIntegrationNotFoundException;
import com.tissue.feature.workflow.domain.VcsAutomationSettings;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.dto.IssueIdentifier;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VcsIntegrationServiceTest {

    @InjectMocks
    private VcsIntegrationService sut;

    @Mock
    private IssueTransitionService issueTransitionService;

    @Mock
    private WorkspaceVcsIntegrationRepository integrationRepository;

    @Mock
    private IssueQueryRepository issueQueryRepository;

    @Mock
    private ProjectMemberQueryRepository projectMemberQueryRepository;

    @Mock
    private IssueEventPublisher eventPublisher;

    @Mock
    private Issue issue;

    @Mock
    private IssueType issueType;

    @Mock
    private Workflow workflow;

    @Mock
    private VcsAutomationSettings vcsSettings;

    @Mock
    private WorkflowState currentState;

    @Mock
    private WorkflowState targetState;

    @Mock
    private WorkflowTransition transition;

    @Mock
    private com.tissue.shared.vo.Name stateName;

    @Mock
    private WorkspaceVcsIntegration integration;

    @Mock
    private ProjectMember projectMember;

    @Mock
    private WorkspaceMember workspaceMember;

    @Mock
    private Project project;

    @Mock
    private Workspace workspace;

    private final String workspaceKey = "WS-KEY";
    private final String projectKey = "PROJ";
    private final String issueKey = "PROJ-123";
    private final String email = "test@example.com";

    @Nested
    @DisplayName("handle pull request")
    class HandlePullRequest {

        @Test
        @DisplayName("success: transitions issue when user is matched")
        void success_UserMatched() {
            GitPrDto prDto = createPrDto(PrAction.OPENED, "Fix bug for PROJ-123");

            given(integrationRepository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));
            given(integration.isActive()).willReturn(true);

            given(issueQueryRepository.findByKeyAndWorkspaceKey(issueKey, workspaceKey))
                    .willReturn(Optional.of(issue));

            given(issue.getKey()).willReturn(issueKey);
            given(issue.getProjectKey()).willReturn(projectKey);
            given(issue.getWorkspaceKey()).willReturn(workspaceKey);
            given(issue.getIssueType()).willReturn(issueType);
            given(issueType.getWorkflow()).willReturn(workflow);
            given(workflow.getVcsSettings()).willReturn(vcsSettings);
            given(vcsSettings.getVcsPrOpenedTransition()).willReturn(transition);

            given(issue.getCurrentState()).willReturn(currentState);
            given(currentState.getName()).willReturn(stateName);
            given(targetState.getName()).willReturn(stateName);
            given(stateName.getDisplay()).willReturn("Some State");
            given(transition.getSourceState()).willReturn(currentState);
            given(transition.getId()).willReturn(100L);
            given(transition.getTargetState()).willReturn(targetState);

            // mock project member structure for context creation
            given(projectMemberQueryRepository.findWithWorkspaceMemberByEmailAndKeys(email, projectKey, workspaceKey))
                    .willReturn(Optional.of(projectMember));
            given(projectMember.getWorkspaceMember()).willReturn(workspaceMember);
            given(workspaceMember.getWorkspace()).willReturn(workspace);
            given(workspace.getId()).willReturn(1L);
            given(projectMember.getProject()).willReturn(project);
            given(project.getId()).willReturn(2L);
            given(projectMember.getId()).willReturn(10L);
            given(projectMember.getMemberId()).willReturn(100L);
            given(projectMember.getProjectKey()).willReturn(projectKey);
            given(projectMember.getWorkspaceKey()).willReturn(workspaceKey);

            given(workspaceMember.getDisplayName()).willReturn("Test User");
            given(workspaceMember.getRole()).willReturn(WorkspaceRole.MEMBER);

            sut.handlePullRequest(prDto);

            then(eventPublisher).should().publishVcsConnectionEvent(issue, prDto, projectMember);
            then(issueTransitionService).should().performTransition(any(IssueIdentifier.class), eq(100L), eq(100L));
            then(issueTransitionService).should(never()).performTransitionBySystem(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("success: transitions issue by system when user is not matched")
        void success_UserNotMatched() {
            GitPrDto prDto = createPrDto(PrAction.MERGED, "Merge: PROJ-123");

            given(integrationRepository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));
            given(integration.isActive()).willReturn(true);
            given(issueQueryRepository.findByKeyAndWorkspaceKey(issueKey, workspaceKey))
                    .willReturn(Optional.of(issue));

            given(issue.getKey()).willReturn(issueKey);
            given(issue.getProjectKey()).willReturn(projectKey);
            given(issue.getWorkspaceKey()).willReturn(workspaceKey);
            given(issue.getIssueType()).willReturn(issueType);
            given(issueType.getWorkflow()).willReturn(workflow);
            given(workflow.getVcsSettings()).willReturn(vcsSettings);
            given(vcsSettings.getVcsPrMergedTransition()).willReturn(transition);

            given(issue.getCurrentState()).willReturn(currentState);
            given(currentState.getName()).willReturn(stateName);
            given(targetState.getName()).willReturn(stateName);
            given(stateName.getDisplay()).willReturn("Some State");
            given(transition.getSourceState()).willReturn(currentState);
            given(transition.getId()).willReturn(200L);
            given(transition.getTargetState()).willReturn(targetState);

            given(projectMemberQueryRepository.findWithWorkspaceMemberByEmailAndKeys(email, projectKey, workspaceKey))
                    .willReturn(Optional.empty());

            sut.handlePullRequest(prDto);

            then(eventPublisher).should().publishVcsConnectionEvent(issue, prDto, null);
            then(issueTransitionService).should(never()).performTransition(any(), any(), any());
            then(issueTransitionService)
                    .should()
                    .performTransitionBySystem(
                            eq(issueKey),
                            eq(200L),
                            eq(workspaceKey),
                            eq(projectKey),
                            any(PerformSystemTransitionCommand.class));
        }

        @Test
        @DisplayName("fail: integration not found")
        void fail_IntegrationNotFound() {
            GitPrDto prDto = createPrDto(PrAction.OPENED, "PROJ-123");
            given(integrationRepository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> sut.handlePullRequest(prDto))
                    .isInstanceOf(WorkspaceVcsIntegrationNotFoundException.class);
        }

        @Test
        @DisplayName("ignore: integration inactive")
        void ignore_Inactive() {
            GitPrDto prDto = createPrDto(PrAction.OPENED, "PROJ-123");
            given(integrationRepository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));
            given(integration.isActive()).willReturn(false);

            sut.handlePullRequest(prDto);

            then(issueQueryRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("ignore: no issue key in pr title")
        void ignore_NoIssueKey() {
            GitPrDto prDto = createPrDto(PrAction.OPENED, "Just a regular update");
            given(integrationRepository.findByWorkspaceKeyAndProvider(workspaceKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));
            given(integration.isActive()).willReturn(true);

            sut.handlePullRequest(prDto);

            then(issueQueryRepository).shouldHaveNoInteractions();
        }
    }

    private GitPrDto createPrDto(PrAction action, String title) {
        return GitPrDto.builder()
                .workspaceKey(workspaceKey)
                .provider(VcsProvider.GITHUB)
                .action(action)
                .title(title)
                .authorEmail(email)
                .authorUsername("user")
                .occurredAt(Instant.now())
                .htmlUrl("https://github.com/pr/1")
                .build();
    }
}
