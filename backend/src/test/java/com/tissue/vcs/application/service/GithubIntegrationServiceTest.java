package com.tissue.vcs.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.tissue.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.application.service.IssueTransitionService;
import com.tissue.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.issue.domain.Issue;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.vcs.application.dto.GitPrDto;
import com.tissue.vcs.application.port.out.WorkspaceVcsIntegrationRepository;
import com.tissue.vcs.domain.WorkspaceVcsIntegration;
import com.tissue.vcs.domain.enums.PrAction;
import com.tissue.vcs.domain.enums.VcsProvider;
import com.tissue.vcs.domain.exception.WorkspaceVcsIntegrationNotFoundException;
import com.tissue.workflow.domain.VcsAutomationSettings;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
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
class GithubIntegrationServiceTest {

    @InjectMocks
    private GithubIntegrationService sut;

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

            then(eventPublisher).should().publishVcsConnectionEvent(issue, prDto, 100L, "Test User");
            then(issueTransitionService).should().performTransition(any(PerformTransitionCommand.class));
            then(issueTransitionService).should(never()).performTransitionBySystem(any());
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
            given(transition.getSourceState()).willReturn(currentState);
            given(transition.getId()).willReturn(200L);
            given(transition.getTargetState()).willReturn(targetState);

            given(projectMemberQueryRepository.findWithWorkspaceMemberByEmailAndKeys(email, projectKey, workspaceKey))
                    .willReturn(Optional.empty());

            sut.handlePullRequest(prDto);

            then(eventPublisher).should().publishVcsConnectionEvent(issue, prDto, null, null);
            then(issueTransitionService).should(never()).performTransition(any());
            then(issueTransitionService).should().performTransitionBySystem(any(PerformSystemTransitionCommand.class));
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
                .action(action)
                .title(title)
                .authorEmail(email)
                .authorUsername("user")
                .occurredAt(Instant.now())
                .htmlUrl("http://github.com/pr/1")
                .build();
    }
}
