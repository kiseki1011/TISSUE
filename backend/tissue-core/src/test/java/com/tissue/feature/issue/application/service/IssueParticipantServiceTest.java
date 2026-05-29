package com.tissue.feature.issue.application.service;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.MAX_REVIEWERS_EXCEEDED;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueParticipants;
import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.exception.base.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueParticipantServiceTest {

    @Mock
    private IssueFinder issueFinder;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private IssuePolicy issuePolicy;

    @Mock
    private IssueEventPublisher eventPublisher;

    @InjectMocks
    private IssueParticipantService sut;

    @Nested
    @DisplayName("assign issue")
    class AssignIssue {

        @Test
        @DisplayName("success: success adding assignee to issue")
        void sucessAssignIssueToAssignee() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long targetMemberId = 123L;
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            ProjectMember assignee = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);
            Project project = mock(Project.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);
            given(issue.getProject()).willReturn(project);
            given(projectMemberFinder.getBy(issue.getProject(), targetMemberId)).willReturn(assignee);

            // when
            sut.assign(iid, targetMemberId, actorMemberId);

            // then
            then(issue).should().assignTo(assignee);
            then(eventPublisher).should().publishAssigned(issue, assignee, actor);
        }
    }

    @Nested
    @DisplayName("unassign issue")
    class UnassignIssue {

        @Test
        @DisplayName("early-return if assignee is null when unassigning issue")
        void earlyReturn_If_AssigneeNull() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);
            IssueParticipants participants = mock(IssueParticipants.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);
            given(issue.getParticipants()).willReturn(participants);

            // when
            sut.unassign(iid, actorMemberId);

            // then
            then(issue).should(never()).unassign();
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("add issue reviewer")
    class AddIssueReviewer {

        @Test
        @DisplayName("success: success adding reviewer to issue")
        void successAddIssueReviewer() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long targetMemberId = 123L;
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            ProjectMember reviewer = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);
            Project project = mock(Project.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);
            given(issue.getProject()).willReturn(project);
            given(projectMemberFinder.getBy(issue.getProject(), targetMemberId)).willReturn(reviewer);

            // when
            sut.addReviewer(iid, targetMemberId, actorMemberId);

            // then
            then(issuePolicy).should().ensureCanAddReviewer(issue);
            then(issue).should().addReviewer(reviewer);
            then(eventPublisher).should().publishReviewerAdded(issue, reviewer, actor);
        }

        @Test
        @DisplayName("fail: throws BadRequestException if max reviewers exceeded for issue")
        void failAddIssueReviewer_If_MaxReviewerExceeded() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long targetMemberId = 123L;
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);

            willThrow(new BadRequestException(MAX_REVIEWERS_EXCEEDED))
                    .given(issuePolicy)
                    .ensureCanAddReviewer(issue);

            // when & then
            assertThatThrownBy(() -> sut.addReviewer(iid, targetMemberId, actorMemberId))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
