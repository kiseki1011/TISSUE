package com.tissue.feature.vcs.application.service;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.tissue.feature.issue.domain.IssueBranch;
import com.tissue.feature.issue.domain.service.IssueBranchSyncService;
import com.tissue.feature.issue.domain.service.IssuePullRequestSyncService;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.application.dto.GitPushDto;
import com.tissue.feature.vcs.application.dto.VcsEventResult;
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

    @Mock
    private IssuePullRequestSyncService issuePullRequestSyncService;

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

        @Test
        @DisplayName("ignore: an event that moves nothing records no activity")
        void unhandledActionRecordsNoActivity() {
            // given: GitHub sends this for a new commit pushed to an open PR
            GitPrDto prDto = createPrDto(PrAction.UNKNOWN, "Fix bug for PROJ-123");
            ProjectVcsIntegration integration = mock(ProjectVcsIntegration.class);
            given(integrationRepository.findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            Issue issue = mock(Issue.class);
            given(issueQueryRepository.findByKey(issueKey)).willReturn(Optional.of(issue));
            given(issue.getProjectKey()).willReturn(projectKey);
            given(issue.getKey()).willReturn(issueKey);

            // when
            VcsEventResult result = sut.handlePullRequest(prDto);

            // then: the pull request is still kept current, only the feed stays quiet
            assertThat(result.handled()).isTrue();
            then(issuePullRequestSyncService).should().syncPullRequest(eq(issue), eq(prDto));
            then(eventPublisher).should(never()).publishVcsConnectionEvent(any(), any(), any());
        }

        @Test
        @DisplayName("ignore: issue key belongs to another project")
        void ignore_If_IssueBelongsToAnotherProject() {
            // given
            GitPrDto prDto = createPrDto(PrAction.OPENED, "Fix bug for PROJ-123");
            ProjectVcsIntegration integration = mock(ProjectVcsIntegration.class);
            given(integrationRepository.findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            Issue issue = mock(Issue.class);
            given(issueQueryRepository.findByKey(issueKey)).willReturn(Optional.of(issue));
            given(issue.getProjectKey()).willReturn("OTHER");

            // when
            VcsEventResult result = sut.handlePullRequest(prDto);

            // then
            assertThat(result.handled()).isFalse();
            then(eventPublisher).should(never()).publishVcsConnectionEvent(any(), any(), any());
            then(issueTransitionService).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("handle push")
    class HandlePush {

        @Test
        @DisplayName("success: the push that links a branch records activity")
        void recordsActivityWhenBranchNewlyLinked() {
            // given
            Issue issue = givenIssueForPush();
            IssueBranch branch = mock(IssueBranch.class);
            given(branch.getBranchName()).willReturn("feature/PROJ-123");
            given(issueBranchSyncService.syncBranch(eq(issue), any(GitPushDto.class)))
                    .willReturn(new IssueBranchSyncService.BranchSync(branch, true));

            // when
            VcsEventResult result = sut.handlePushEvent(createPushDto("refs/heads/feature/PROJ-123"));

            // then
            assertThat(result.handled()).isTrue();
            assertThat(result.detail()).contains("Linked branch");
            then(eventPublisher).should().publishBranchLinked(eq(issue), eq(branch), any());
        }

        @Test
        @DisplayName("ignore: a later push to the same branch records no activity")
        void recordsNoActivityWhenBranchOnlyMoved() {
            // given
            Issue issue = givenIssueForPush();
            IssueBranch branch = mock(IssueBranch.class);
            given(branch.getBranchName()).willReturn("feature/PROJ-123");
            given(issueBranchSyncService.syncBranch(eq(issue), any(GitPushDto.class)))
                    .willReturn(new IssueBranchSyncService.BranchSync(branch, false));

            // when
            VcsEventResult result = sut.handlePushEvent(createPushDto("refs/heads/feature/PROJ-123"));

            // then: still handled - the branch moved - but the feed stays quiet
            assertThat(result.handled()).isTrue();
            assertThat(result.detail()).contains("Updated branch");
            then(eventPublisher).should(never()).publishBranchLinked(any(), any(), any());
        }

        private Issue givenIssueForPush() {
            ProjectVcsIntegration integration = mock(ProjectVcsIntegration.class);
            given(integrationRepository.findByProjectKeyAndProvider(projectKey, VcsProvider.GITHUB))
                    .willReturn(Optional.of(integration));

            Issue issue = mock(Issue.class);
            given(issueQueryRepository.findByKey(issueKey)).willReturn(Optional.of(issue));
            given(issue.getProjectKey()).willReturn(projectKey);
            given(issue.getKey()).willReturn(issueKey);
            return issue;
        }
    }

    private GitPushDto createPushDto(String ref) {
        return GitPushDto.builder()
                .projectKey(projectKey)
                .provider(VcsProvider.GITHUB)
                .ref(ref)
                .repoUrl("https://github.com/acme/repo")
                .pusherName("user")
                .pusherEmail(email)
                .latestCommitHash("abc1234")
                .latestCommitMessage("work")
                .latestCommitUrl("https://github.com/acme/repo/commit/abc1234")
                .occurredAt(Instant.now())
                .build();
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
