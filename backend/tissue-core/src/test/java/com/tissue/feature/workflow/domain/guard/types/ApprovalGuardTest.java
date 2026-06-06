package com.tissue.feature.workflow.domain.guard.types;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.domain.exception.ChangeRequestBlockedException;
import com.tissue.feature.workflow.domain.exception.InsufficientApprovalsException;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.support.TestFixtures;
import java.math.RoundingMode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApprovalGuardTest {

    private final IssuePolicy issuePolicy = new IssuePolicy(10, 0, RoundingMode.HALF_UP, 3, 0, 50);
    private final ApprovalGuard guard = new ApprovalGuard(issuePolicy);

    @Test
    @DisplayName("fail: a reviewer who requested changes blocks the transition")
    void blocksOnChangeRequest() {
        // given
        Issue issue = issueWithReviewer();
        reviewerOf(issue).reject(); // CHANGES_REQUESTED

        // when & then
        assertThatThrownBy(() -> guard.evaluate(context(issue, Map.of())))
                .isInstanceOfSatisfying(ChangeRequestBlockedException.class, ex -> assertThat(ex.getDetails())
                        .containsEntry("changeRequestedCount", 1));
    }

    @Test
    @DisplayName("fail: fewer approvals than required blocks transition and reports counts")
    void blocksOnInsufficientApprovals() {
        // given
        Issue issue = issueWithReviewer();
        reviewerOf(issue).approve(); // 1 approval

        // when & then - 2 approvals required
        assertThatThrownBy(() -> guard.evaluate(context(issue, Map.of("min_approvals", 2))))
                .isInstanceOfSatisfying(InsufficientApprovalsException.class, ex -> {
                    assertThat(ex.getDetails()).containsEntry("currentApprovals", 1);
                    assertThat(ex.getDetails()).containsEntry("requiredApprovals", 2);
                });
    }

    @Test
    @DisplayName("success: evaluation success if enough approvals and no change requests")
    void allowsWhenEnoughApprovals() {
        // given
        Issue issue = issueWithReviewer();
        reviewerOf(issue).approve();

        // when & then
        assertThatCode(() -> guard.evaluate(context(issue, Map.of("min_approvals", 1))))
                .doesNotThrowAnyException();
    }

    private Issue issueWithReviewer() {
        Project project = TestFixtures.project("PROJ");
        Issue issue = TestFixtures.issue(project, "test", IssueHierarchy.STANDARD);
        ProjectMember reviewer = TestFixtures.projectMember(project, TestFixtures.member("reviewer"));
        issue.addReviewer(reviewer);
        return issue;
    }

    private IssueReviewer reviewerOf(Issue issue) {
        return issue.getParticipants().getReviewers().iterator().next();
    }

    private GuardContext context(Issue issue, Map<String, Object> params) {
        return GuardContext.builder()
                .issue(issue)
                .params(params)
                .projectKey("PROJ")
                .build();
    }
}
