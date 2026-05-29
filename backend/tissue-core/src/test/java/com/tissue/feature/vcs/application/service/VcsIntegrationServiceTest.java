package com.tissue.feature.vcs.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.application.dto.request.PerformSystemTransitionCommand;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.service.IssueTransitionService;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.service.IssueBranchSyncService;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.application.port.repository.ProjectVcsIntegrationRepository;
import com.tissue.feature.vcs.domain.ProjectVcsIntegration;
import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.workflow.domain.VcsAutomationSettings;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.vo.Name;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VcsIntegrationServiceTest {

    @Mock
    private IssueTransitionService issueTransitionService;

    @Mock
    private ProjectVcsIntegrationRepository integrationRepository;

    @Mock
    private IssueQueryRepository issueQueryRepository;

    @Mock
    private ProjectMemberQueryRepository projectMemberQueryRepository;

    @Mock
    private IssueEventPublisher eventPublisher;

    @Mock
    private IssueBranchSyncService issueBranchSyncService;

    @InjectMocks
    private VcsIntegrationService sut;

    private final String projectKey = "PROJ";
    private final String issueKey = "PROJ-123";
    private final String email = "test@example.com";

    @Nested
    @DisplayName("handle pull request")
    class HandlePullRequest {

        @Test
        @DisplayName("success: transitions issue by matched member")
        void successTransitionByMatchedMember() {
            // given
            GitPrDto prDto = createPrDto(PrAction.OPENED, "Fix bug for PROJ-123");

            ProjectVcsIntegration integration = mock(ProjectVcsIntegration.class);
            given(integrationRepository.findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            Issue issue = mock(Issue.class);
            given(issueQueryRepository.findByKey(issueKey)).willReturn(Optional.of(issue));
            given(issue.getKey()).willReturn(issueKey);
            given(issue.getProjectKey()).willReturn(projectKey);
            IssueType issueType = mock(IssueType.class);
            Workflow workflow = mock(Workflow.class);
            VcsAutomationSettings vcsSettings = mock(VcsAutomationSettings.class);
            WorkflowTransition transition = mock(WorkflowTransition.class);
            given(issue.getIssueType()).willReturn(issueType);
            given(issueType.getWorkflow()).willReturn(workflow);
            given(workflow.getVcsSettings()).willReturn(vcsSettings);
            given(vcsSettings.getVcsPrOpenedTransition()).willReturn(transition);

            WorkflowState currentState = mock(WorkflowState.class);
            given(issue.getCurrentState()).willReturn(currentState);
            given(transition.getSourceState()).willReturn(currentState);
            given(transition.getId()).willReturn(100L);

            WorkflowState targetState = mock(WorkflowState.class);
            Name stateName = mock(Name.class);
            given(transition.getTargetState()).willReturn(targetState);
            given(currentState.getName()).willReturn(stateName);
            given(targetState.getName()).willReturn(stateName);
            given(stateName.getDisplayName()).willReturn("In Progress");

            ProjectMember actor = mock(ProjectMember.class);
            given(projectMemberQueryRepository.findWithMemberByEmailAndProjectKey(email, projectKey))
                    .willReturn(Optional.of(actor));
            given(actor.getMemberId()).willReturn(100L);
            given(actor.getDisplayName()).willReturn("Test User");

            // when
            sut.handlePullRequest(prDto);

            // then
            then(eventPublisher).should().publishVcsConnectionEvent(issue, prDto, actor);
            then(issueTransitionService).should().performTransition(any(IssueIdentifier.class), eq(100L), eq(100L));
            then(issueTransitionService).should(never()).performTransitionBySystem(any(), any(), any());
        }

        @Test
        @DisplayName("success: transitions issue by system when no member matched")
        void successTransitionBySystem() {
            // given
            GitPrDto prDto = createPrDto(PrAction.MERGED, "Merge: PROJ-123");

            ProjectVcsIntegration integration = mock(ProjectVcsIntegration.class);
            given(integrationRepository.findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            Issue issue = mock(Issue.class);
            given(issueQueryRepository.findByKey(issueKey)).willReturn(Optional.of(issue));
            given(issue.getKey()).willReturn(issueKey);
            given(issue.getProjectKey()).willReturn(projectKey);
            IssueType issueType = mock(IssueType.class);
            Workflow workflow = mock(Workflow.class);
            VcsAutomationSettings vcsSettings = mock(VcsAutomationSettings.class);
            WorkflowTransition transition = mock(WorkflowTransition.class);
            given(issue.getIssueType()).willReturn(issueType);
            given(issueType.getWorkflow()).willReturn(workflow);
            given(workflow.getVcsSettings()).willReturn(vcsSettings);
            given(vcsSettings.getVcsPrMergedTransition()).willReturn(transition);

            WorkflowState currentState = mock(WorkflowState.class);
            given(issue.getCurrentState()).willReturn(currentState);
            given(transition.getSourceState()).willReturn(currentState);
            given(transition.getId()).willReturn(200L);

            WorkflowState targetState = mock(WorkflowState.class);
            Name stateName = mock(Name.class);
            given(transition.getTargetState()).willReturn(targetState);
            given(currentState.getName()).willReturn(stateName);
            given(targetState.getName()).willReturn(stateName);
            given(stateName.getDisplayName()).willReturn("Done");

            given(projectMemberQueryRepository.findWithMemberByEmailAndProjectKey(email, projectKey))
                    .willReturn(Optional.empty());

            // when
            sut.handlePullRequest(prDto);

            // then
            then(eventPublisher).should().publishVcsConnectionEvent(issue, prDto, null);
            then(issueTransitionService).should(never()).performTransition(any(), any(), any());
            then(issueTransitionService)
                    .should()
                    .performTransitionBySystem(eq(issueKey), eq(200L), any(PerformSystemTransitionCommand.class));
        }

        @Test
        @DisplayName("ignore: skips transition when current state does not match source state")
        void ignoreTransition_If_StateMismatch() {
            // given
            GitPrDto prDto = createPrDto(PrAction.OPENED, "Fix bug for PROJ-123");

            ProjectVcsIntegration integration = mock(ProjectVcsIntegration.class);
            given(integrationRepository.findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            Issue issue = mock(Issue.class);
            given(issueQueryRepository.findByKey(issueKey)).willReturn(Optional.of(issue));
            given(issue.getProjectKey()).willReturn(projectKey);
            IssueType issueType = mock(IssueType.class);
            Workflow workflow = mock(Workflow.class);
            VcsAutomationSettings vcsSettings = mock(VcsAutomationSettings.class);
            WorkflowTransition transition = mock(WorkflowTransition.class);
            given(issue.getIssueType()).willReturn(issueType);
            given(issueType.getWorkflow()).willReturn(workflow);
            given(workflow.getVcsSettings()).willReturn(vcsSettings);
            given(vcsSettings.getVcsPrOpenedTransition()).willReturn(transition);

            WorkflowState currentState = mock(WorkflowState.class);
            WorkflowState sourceState = mock(WorkflowState.class);
            given(issue.getCurrentState()).willReturn(currentState);
            given(transition.getSourceState()).willReturn(sourceState);

            Name currentName = mock(Name.class);
            Name sourceName = mock(Name.class);
            given(currentState.getName()).willReturn(currentName);
            given(sourceState.getName()).willReturn(sourceName);
            given(currentName.getDisplayName()).willReturn("Done");
            given(sourceName.getDisplayName()).willReturn("To Do");

            // when
            sut.handlePullRequest(prDto);

            // then
            then(eventPublisher).should().publishVcsConnectionEvent(eq(issue), eq(prDto), any());
            then(issueTransitionService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("ignore: no integration found for project")
        void ignore_If_IntegrationNotFound() {
            // given
            GitPrDto prDto = createPrDto(PrAction.OPENED, "PROJ-123");
            given(integrationRepository.findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB))
                    .willReturn(Optional.empty());

            // when
            sut.handlePullRequest(prDto);

            // then
            then(issueQueryRepository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("ignore: integration is inactive")
        void ignore_If_IntegrationInactive() {
            // given
            GitPrDto prDto = createPrDto(PrAction.OPENED, "PROJ-123");
            ProjectVcsIntegration integration = mock(ProjectVcsIntegration.class);
            given(integrationRepository.findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));
            given(integration.isInactive()).willReturn(true);

            // when
            sut.handlePullRequest(prDto);

            // then
            then(issueQueryRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("ignore: no issue key found in PR title")
        void ignore_If_NoIssueKeyInTitle() {
            // given
            GitPrDto prDto = createPrDto(PrAction.OPENED, "Just a regular update");
            ProjectVcsIntegration integration = mock(ProjectVcsIntegration.class);
            given(integrationRepository.findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            // when
            sut.handlePullRequest(prDto);

            // then
            then(issueQueryRepository).shouldHaveNoInteractions();
        }
    }

    private GitPrDto createPrDto(PrAction action, String title) {
        return GitPrDto.builder()
                .projectKey(projectKey)
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
