package com.tissue.feature.issue.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.support.TestFixtures;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class IssueParticipantsTest {

    @Nested
    @DisplayName("add reviewer")
    class AddReviewer {

        @Test
        @DisplayName("success: add reviewer to issue")
        void successAddReviewer() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue issue = TestFixtures.issue(project, "test", IssueHierarchy.STANDARD);
            Member member = TestFixtures.member("reviewer");
            ProjectMember reviewer = TestFixtures.projectMember(project, member);

            // when
            issue.addReviewer(reviewer);

            // then
            assertThat(issue.getParticipants().getReviewers()).hasSize(1);
        }

        @Test
        @DisplayName("success: adding same reviewer is idempotent")
        void successAddReviewerIdempotent() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue issue = TestFixtures.issue(project, "test", IssueHierarchy.STANDARD);
            Member member = TestFixtures.member("reviewer");
            ProjectMember reviewer = TestFixtures.projectMember(project, member);

            // when
            issue.addReviewer(reviewer);
            issue.addReviewer(reviewer);

            // then
            assertThat(issue.getParticipants().getReviewers()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("add subscriber")
    class AddSubscriber {

        @Test
        @DisplayName("success: adding same subscriber twice is idempotent")
        void successAddSubscriberIdempotent() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue issue = TestFixtures.issue(project, "test", IssueHierarchy.STANDARD);
            Member member = TestFixtures.member("subscriber");
            ProjectMember subscriber = TestFixtures.projectMember(project, member);

            // when
            issue.addSubscriber(subscriber);
            issue.addSubscriber(subscriber);

            // then
            assertThat(issue.getParticipants().getSubscribers()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("reset reviews")
    class ResetReviews {

        @Test
        @DisplayName("success: resets CHANGES_REQUESTED reviewers when no specific ids given")
        void successResetChangesRequestedReviewers() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue issue = TestFixtures.issue(project, "test", IssueHierarchy.STANDARD);
            Member member = TestFixtures.member("reviewer");
            ProjectMember pm = TestFixtures.projectMember(project, member);

            // when
            issue.addReviewer(pm);

            IssueReviewer reviewer =
                    issue.getParticipants().getReviewers().iterator().next();
            reviewer.reject();

            // then
            assertThat(reviewer.getStatus()).isEqualTo(ReviewStatus.CHANGES_REQUESTED);

            int count = issue.resetReviews(Set.of());
            assertThat(count).isEqualTo(1);
            assertThat(reviewer.getStatus()).isEqualTo(ReviewStatus.PENDING);
        }

        @Test
        @DisplayName("success: does not reset APPROVED reviewers when no specific ids given")
        void successSkipApprovedReviewers() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue issue = TestFixtures.issue(project, "test", IssueHierarchy.STANDARD);
            Member member = TestFixtures.member("reviewer");
            ProjectMember pm = TestFixtures.projectMember(project, member);

            // when
            issue.addReviewer(pm);
            IssueReviewer reviewer =
                    issue.getParticipants().getReviewers().iterator().next();
            reviewer.approve();

            // then
            int count = issue.resetReviews(Set.of());
            assertThat(count).isEqualTo(0);
            assertThat(reviewer.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        }

        @Test
        @DisplayName("success: does not reset PENDING reviewers when no specific ids given")
        void successSkipPendingReviewers() {
            // given
            Project project = TestFixtures.project("PROJ");
            Issue issue = TestFixtures.issue(project, "test", IssueHierarchy.STANDARD);
            Member member = TestFixtures.member("reviewer");
            ProjectMember pm = TestFixtures.projectMember(project, member);

            // when
            issue.addReviewer(pm);

            // then
            int count = issue.resetReviews(Set.of());
            assertThat(count).isEqualTo(0);
        }
    }
}
