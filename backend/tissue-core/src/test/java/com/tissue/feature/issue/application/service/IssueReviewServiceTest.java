package com.tissue.feature.issue.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.comment.application.port.usecase.CommentCommandUseCase;
import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.application.service.publisher.IssueEventPublisher;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueParticipants;
import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IssueReviewServiceTest {

    @Mock
    private IssueFinder issueFinder;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private IssueEventPublisher eventPublisher;

    @Mock
    private CommentCommandUseCase commentCommandUseCase;

    @InjectMocks
    private IssueReviewService sut;

    @Nested
    @DisplayName("submit issue review")
    class SubmitIssueReview {

        @Test
        @DisplayName("success: submit review as approved")
        void successApprovedReviewSubmit() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);
            IssueParticipants participants = mock(IssueParticipants.class);
            IssueReviewer reviewer = mock(IssueReviewer.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);
            given(issue.getParticipants()).willReturn(participants);
            given(participants.getReviewers()).willReturn(Set.of(reviewer));
            given(reviewer.getReviewer()).willReturn(actor);
            given(reviewer.getStatus()).willReturn(ReviewStatus.APPROVED);

            // when
            sut.submitReview(iid, true, null, actorMemberId);

            // then
            then(reviewer).should().approve();
            then(eventPublisher).should().publishReviewSubmitted(issue, ReviewStatus.APPROVED, actor);
            then(commentCommandUseCase).should(never()).createReview(any(), any(), any(), anyLong());
        }

        @Test
        @DisplayName("success: feedback is stored as a comment stamped with the verdict")
        void successReviewWithFeedback() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);
            IssueParticipants participants = mock(IssueParticipants.class);
            IssueReviewer reviewer = mock(IssueReviewer.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);
            given(issue.getParticipants()).willReturn(participants);
            given(participants.getReviewers()).willReturn(Set.of(reviewer));
            given(reviewer.getReviewer()).willReturn(actor);
            given(reviewer.getStatus()).willReturn(ReviewStatus.CHANGES_REQUESTED);

            // when
            sut.submitReview(iid, false, "rename the method", actorMemberId);

            // then
            then(reviewer).should().reject();
            then(commentCommandUseCase)
                    .should()
                    .createReview(iid, "rename the method", ReviewStatus.CHANGES_REQUESTED, actorMemberId);
        }

        @Test
        @DisplayName("success: a blank feedback body leaves only the status change")
        void blankFeedbackCreatesNoComment() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);
            IssueParticipants participants = mock(IssueParticipants.class);
            IssueReviewer reviewer = mock(IssueReviewer.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);
            given(issue.getParticipants()).willReturn(participants);
            given(participants.getReviewers()).willReturn(Set.of(reviewer));
            given(reviewer.getReviewer()).willReturn(actor);
            given(reviewer.getStatus()).willReturn(ReviewStatus.APPROVED);

            // when
            sut.submitReview(iid, true, "   ", actorMemberId);

            // then
            then(commentCommandUseCase).should(never()).createReview(any(), any(), any(), anyLong());
        }
    }

    @Nested
    @DisplayName("request issue review")
    class RequestIssueReview {

        @Test
        @DisplayName("success: request review to reviewers")
        void successReviewRequest() {
            // given
            IssueIdentifier iid = new IssueIdentifier("PROJ", "PROJ-1");
            Long actorMemberId = 1L;
            Set<Long> reviewerMemberIds = Set.of(2L, 3L);

            ProjectMember actor = mock(ProjectMember.class);
            Issue issue = mock(Issue.class);

            given(projectMemberFinder.getByProjectKey(iid.projectKey(), actorMemberId))
                    .willReturn(actor);
            given(issueFinder.getWithProjectByIssueKey(iid.issueKey())).willReturn(issue);
            given(issue.resetReviews(reviewerMemberIds)).willReturn(2);

            // when
            sut.requestReview(iid, reviewerMemberIds, actorMemberId);

            // then
            then(issue).should().resetReviews(reviewerMemberIds);
            then(eventPublisher).should().publishReviewRequested(issue, actor, reviewerMemberIds, 2);
        }
    }
}
