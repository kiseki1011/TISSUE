package com.tissue.mcp.tool;

import com.tissue.feature.comment.application.dto.request.CreateCommentCommand;
import com.tissue.feature.comment.application.dto.response.CommentCreateResponse;
import com.tissue.feature.comment.application.port.usecase.CommentCommandUseCase;
import com.tissue.feature.issue.application.dto.request.CreateIssueCommand;
import com.tissue.feature.issue.application.dto.response.IssueCreateResponse;
import com.tissue.feature.issue.application.dto.response.IssueDetail;
import com.tissue.feature.issue.application.port.usecase.IssueLifecycleUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueParticipantUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueTransitionUseCase;
import com.tissue.feature.issue.domain.enums.IssuePriority;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Most logic are just wrappers",
        model = "claude-opus-4-8")
@Component
@RequiredArgsConstructor
public class IssueWriteTool {

    private final IssueLifecycleUseCase issueLifecycleUseCase;
    private final IssueTransitionUseCase issueTransitionUseCase;
    private final IssueParticipantUseCase issueParticipantUseCase;
    private final CommentCommandUseCase commentCommandUseCase;
    private final IssueQueryUseCase issueQueryUseCase;

    @McpTool(name = "create_issue", description = """
            Create a new issue in a project. Call get_issue_types first to choose an issueTypeId and learn which \
            custom fields the type requires. The issue starts in its workflow's initial state and is unscheduled \
            (no sprint), so it lands in the backlog. Returns the new issue key (ex: "PROJ-123"). You usually \
            call claim_issue next (to set yourself as the assignee), then transition it as you make progress.""")
    public IssueCreateResponse createIssue(
            @McpToolParam(required = true, description = "The project key to create the issue in. ex: \"PROJ\".")
                    String projectKey,
            @McpToolParam(required = true, description = "The issue type id, from get_issue_types.") Long issueTypeId,
            @McpToolParam(required = true, description = "The issue title.") String title,
            @McpToolParam(required = true, description = "Priority, one of P0 (blocker), P1, P2, P3, P4 (trivial).")
                    IssuePriority priority,
            @McpToolParam(required = false, description = "The issue body/description.") @Nullable String content,
            @McpToolParam(required = false, description = "A short summary of the issue.") @Nullable String summary,
            @McpToolParam(required = false, description = "Estimated story points (only for STANDARD hierarchy).")
                    @Nullable
                    Integer storyPoint,
            @McpToolParam(
                            required = false,
                            description = "Due date as an ISO-8601 instant, ex: \"2026-01-31T17:00:00Z\".")
                    @Nullable
                    String dueAt,
            @McpToolParam(required = false, description = "Member id to assign on creation. Omit to leave unassigned.")
                    @Nullable
                    Long assigneeMemberId,
            @McpToolParam(required = false, description = """
                                    Custom field values for the chosen issue type. A map of field id (the id from \
                                    get_issue_types, as a string key) to value. Include every field whose required \
                                    flag is true. Unknown field ids are ignored. Value by field type:
                                    - TEXT: a string
                                    - INTEGER / DECIMAL / PERCENTAGE: a number
                                    - BOOLEAN: true or false
                                    - DATE / TIMESTAMP: an ISO-8601 string
                                    - SELECT_OPTION: a single option id (number)
                                    - CHECKLIST: a map of option id to true/false""") @Nullable Map<String, Object> customFields) {
        McpActor.requireWriteScope();

        CreateIssueCommand command = CreateIssueCommand.builder()
                .title(title)
                .content(content)
                .summary(summary)
                .priority(priority)
                .dueAt(parseInstant(dueAt))
                .storyPoint(storyPoint)
                .issueTypeId(issueTypeId)
                .customFields(toCustomFields(customFields))
                .assigneeMemberId(assigneeMemberId)
                .build();

        return issueLifecycleUseCase.create(
                ProjectIdentifier.ofProjectKey(projectKey), command, McpActor.currentMemberId());
    }

    @McpTool(name = "transition_issue", description = """
            Move an issue to a new workflow state by executing a transition. Call list_available_transitions \
            first to discover valid transitions and to confirm the one you want is not blocked by a guard \
            (ex: a required approval or an unresolved blocking issue). Returns the issue's full detail with its \
            new current state.""")
    public IssueDetail transitionIssue(
            @McpToolParam(required = true, description = "The issue key. ex: \"PROJ-123\".") String issueKey,
            @McpToolParam(required = true, description = "The transition id, from list_available_transitions.")
                    Long transitionId) {
        McpActor.requireWriteScope();

        issueTransitionUseCase.performTransition(
                IssueIdentifier.ofIssueKey(issueKey), transitionId, McpActor.currentMemberId());

        return getDetail(issueKey);
    }

    @McpTool(name = "claim_issue", description = """
            Assign an unassigned issue to yourself, to pick up work before acting on it. Fails with a conflict \
            if the issue is already assigned to someone else. Re-claiming an issue you already hold does nothing. \
            Returns the issue's full detail with you as the assignee.""")
    public IssueDetail claimIssue(
            @McpToolParam(required = true, description = "The issue key to claim. ex: \"PROJ-123\".") String issueKey) {
        McpActor.requireWriteScope();

        issueParticipantUseCase.claim(IssueIdentifier.ofIssueKey(issueKey), McpActor.currentMemberId());

        return getDetail(issueKey);
    }

    @McpTool(name = "assign_issue", description = """
            Assign an issue to a specific member, replacing any current assignee (a forceful assignment). The new \
            assignee is automatically removed from the issue's reviewer list. To take an issue yourself, prefer \
            claim_issue. Returns the issue's full detail with the new assignee.""")
    public IssueDetail assignIssue(
            @McpToolParam(required = true, description = "The issue key. ex: \"PROJ-123\".") String issueKey,
            @McpToolParam(
                            required = true,
                            description = "The id of the member to assign the issue to; "
                                    + "find member ids via get_my_work or get_issue.")
                    Long memberId) {
        McpActor.requireWriteScope();
        issueParticipantUseCase.assign(IssueIdentifier.ofIssueKey(issueKey), memberId, McpActor.currentMemberId());
        return getDetail(issueKey);
    }

    @McpTool(name = "add_comment", description = """
            Add a comment to an issue to leave progress notes, ask questions, or hand off work. \
            Returns the new comment id.""")
    public CommentCreateResponse addComment(
            @McpToolParam(required = true, description = "The issue key. ex: \"PROJ-123\".") String issueKey,
            @McpToolParam(required = true, description = "The comment body.") String content,
            @McpToolParam(required = false, description = "Usernames to mention/notify (without the @).") @Nullable
                    List<String> mentionedUsernames,
            @McpToolParam(
                            required = false,
                            description = "Id of the comment to reply to. Omit for a top-level comment.")
                    @Nullable
                    Long parentCommentId) {
        McpActor.requireWriteScope();

        CreateCommentCommand command = CreateCommentCommand.builder()
                .content(content)
                .mentionedUsernames(mentionedUsernames != null ? mentionedUsernames : List.of())
                .parentCommentId(parentCommentId)
                .build();

        return commentCommandUseCase.create(IssueIdentifier.ofIssueKey(issueKey), command, McpActor.currentMemberId());
    }

    private IssueDetail getDetail(String issueKey) {
        return issueQueryUseCase.getDetail(IssueIdentifier.ofIssueKey(issueKey), McpActor.currentMemberId());
    }

    private static @Nullable Instant parseInstant(@Nullable String iso) {
        if (iso == null || iso.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(iso);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(
                    "dueAt must be an ISO-8601 instant in UTC, ex: \"2026-01-31T17:00:00Z\". Got: \"" + iso + "\".");
        }
    }

    private static Map<Long, Object> toCustomFields(@Nullable Map<String, Object> raw) {
        if (raw == null || raw.isEmpty()) {
            return Map.of();
        }
        Map<Long, Object> result = new HashMap<>();
        raw.forEach((key, value) -> result.put(parseFieldId(key), value));
        return result;
    }

    private static Long parseFieldId(String key) {
        try {
            return Long.valueOf(key);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "customFields keys must be numeric field ids from get_issue_types. Got non-numeric key: \"" + key
                            + "\".");
        }
    }
}
