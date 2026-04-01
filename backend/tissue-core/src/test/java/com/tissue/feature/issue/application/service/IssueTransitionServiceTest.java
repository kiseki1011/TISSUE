package com.tissue.feature.issue.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.application.service.validator.IssueValidator;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.service.TransitionGuardRegistry;
import com.tissue.shared.dto.IssueIdentifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueTransitionServiceTest {

    @Mock
    private IssueFinder issueFinder;

    @Mock
    private WorkflowFinder workflowFinder;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private IssueValidator issueValidator;

    @Mock
    private TransitionGuardRegistry guardRegistry;

    @Mock
    private IssueEventPublisher eventPublisher;

    @InjectMocks
    private IssueTransitionService sut;

    @Nested
    @DisplayName("perform issue transition")
    class IssueTransition {

        @Test
        @DisplayName("success: success issue (workflow) transition")
        void successIssueTransition() {
            // given
            IssueIdentifier iid = new IssueIdentifier("WORKSPACE", "PROJ", "PROJ-1");
            Long transitionId = 1L;
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);
            WorkflowState oldState = mock(WorkflowState.class);
            WorkflowState targetState = mock(WorkflowState.class);
            WorkflowTransition transition = mock(WorkflowTransition.class);
            Workflow workflow = mock(Workflow.class);

            given(issueFinder.getWithProjectBy(iid.workspaceKey(), iid.issueKey()))
                    .willReturn(issue);
            given(projectMemberFinder.getWithWorkspaceMember(iid.workspaceKey(), iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issue.getCurrentState()).willReturn(oldState);
            given(oldState.getWorkflow()).willReturn(workflow);
            given(workflow.getId()).willReturn(10L);
            given(workflowFinder.getTransitionWithHierarchyBy(iid.workspaceKey(), 10L, transitionId))
                    .willReturn(transition);
            given(transition.getTargetState()).willReturn(targetState);

            // when
            sut.performTransition(iid, transitionId, actorMemberId);

            // then
            then(issueValidator).should().ensureValidTransition(issue, iid.workspaceKey(), transition);
            then(issue).should().transitionTo(targetState);
            then(eventPublisher).should().publishTransitioned(issue, transition, oldState, actor);
        }
    }
}
