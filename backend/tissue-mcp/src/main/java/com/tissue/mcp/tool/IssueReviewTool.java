package com.tissue.mcp.tool;

import com.tissue.feature.issue.application.port.usecase.IssueParticipantUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueReviewUseCase;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueReviewTool {

    private final IssueReviewUseCase issueReviewUseCase;
    private final IssueParticipantUseCase issueParticipantUseCase;

    @McpTool(name = "add_reviewer", description = """
            Put a member on an issue's reviewer list, so the issue reads as awaiting their review. Call \
            this to start a review loop: nothing else adds a reviewer, and request_review only reaches \
            people who are already on the list.

            The issue's assignee cannot review their own work, so naming them is rejected. Adding \
            someone already on the list does nothing, and an issue can hold only so many reviewers \
            before it is refused.""")
    public void addReviewer(
            @McpToolParam(required = true, description = "The issue key, ex: \"PROJ-123\".") String issueKey,
            @McpToolParam(required = true, description = "Member id of the reviewer to add, from get_project_members.")
                    Long memberId) {
        McpActor.requireWriteScope();

        issueParticipantUseCase.addReviewer(IssueIdentifier.ofIssueKey(issueKey), memberId, McpActor.currentMemberId());
    }

    @McpTool(name = "remove_reviewer", description = """
            Take a member off an issue's reviewer list, dropping the verdict they had given. Removing \
            someone who is not a reviewer does nothing.""")
    public void removeReviewer(
            @McpToolParam(required = true, description = "The issue key, ex: \"PROJ-123\".") String issueKey,
            @McpToolParam(
                            required = true,
                            description = "Member id of the reviewer to remove, from the reviewers on get_issue.")
                    Long memberId) {
        McpActor.requireWriteScope();

        issueParticipantUseCase.removeReviewer(
                IssueIdentifier.ofIssueKey(issueKey), memberId, McpActor.currentMemberId());
    }

    @McpTool(name = "submit_review", description = """
            Record your verdict on an issue you were asked to review. Fails if you are not one of the \
            issue's reviewers - get_issue tells you who they are. A non-blank feedback body is also \
            posted to the issue's comment thread, stamped with the verdict you gave it, so the reasoning \
            stays readable next to the discussion; approving with nothing to say can leave it out.

            Rejecting does not move the issue by itself. It marks your review as changes-requested, which \
            a workflow guard may then use to block a transition until the review passes.""")
    public void submitReview(
            @McpToolParam(required = true, description = "The issue key, ex: \"PROJ-123\".") String issueKey,
            @McpToolParam(required = true, description = "true to approve, false to request changes.") boolean approved,
            @McpToolParam(
                            required = false,
                            description = "Your feedback, in the issue's comment thread. Say what you "
                                    + "found and what you want changed. Omit only when approving with "
                                    + "nothing to add.")
                    @Nullable
                    String feedback) {
        McpActor.requireWriteScope();

        issueReviewUseCase.submitReview(
                IssueIdentifier.ofIssueKey(issueKey), approved, feedback, McpActor.currentMemberId());
    }

    @McpTool(name = "request_review", description = """
            Ask reviewers to look at an issue again, after you have addressed their feedback. It resets \
            the reviews that already have a verdict back to pending, so the issue reads as awaiting \
            review once more.

            This only reaches members who are already reviewers on the issue - use add_reviewer to put \
            someone on the list - and a reviewer whose review is still pending is left alone. Naming \
            nobody resets everyone who requested changes, which is the usual "I fixed it, please \
            re-check" case.""")
    public void requestReview(
            @McpToolParam(required = true, description = "The issue key, ex: \"PROJ-123\".") String issueKey,
            @McpToolParam(
                            required = false,
                            description = "Member ids of the reviewers to ask again, from "
                                    + "get_project_members or the reviewers on get_issue. Omit to ask "
                                    + "everyone who requested changes.")
                    @Nullable
                    List<Long> reviewerMemberIds) {
        McpActor.requireWriteScope();

        issueReviewUseCase.requestReview(
                IssueIdentifier.ofIssueKey(issueKey),
                reviewerMemberIds == null ? Set.of() : Set.copyOf(reviewerMemberIds),
                McpActor.currentMemberId());
    }
}
