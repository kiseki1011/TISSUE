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
        evaluationReason = "most logic are just wrappers",
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
            Create a new issue in a project. Call get_issue_types first to choose an issueTypeId and learn its \
            custom fields. priority is one of P0 (blocker), P1, P2, P3, P4 (trivial). dueAt, if given, is an \
            ISO-8601 instant (ex: "2026-01-31T17:00:00Z"). customFields maps each field id (the id from \
            get_issue_types, as a string key) to its value. You must include every field whose required flag is \
            true. Unknown field ids are ignored. Value by field type: TEXT=string, INTEGER/DECIMAL/PERCENTAGE=number, \
            BOOLEAN=true/false, DATE/TIMESTAMP=ISO-8601 string, SELECT_OPTION=a single option id (number), \
            CHECKLIST=a map of option id to true/false. The issue starts in its workflow's initial state and is \
            unscheduled (no sprint) — i.e. it lands in the backlog. Returns the new issue key (ex: "PROJ-123"); you \
            usually claim_issue it next, then transition it as you make progress.""")
    public IssueCreateResponse createIssue(
            @McpToolParam(required = true, description = "The project key to create the issue in. ex: \"PROJ\".")
                    String projectKey,
            @McpToolParam(required = true, description = "The issue type id, from get_issue_types.") Long issueTypeId,
            @McpToolParam(required = true, description = "The issue title.") String title,
            @McpToolParam(required = true, description = "Priority: one of P0, P1, P2, P3, P4.") IssuePriority priority,
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
            @McpToolParam(
                            required = false,
                            description = "Map of custom field id (from get_issue_types) to value. "
                                    + "Required fields of the chosen type must be present.")
                    @Nullable
                    Map<String, Object> customFields) {
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
            first to get a valid transitionId and to check that it is not blocked (a guard, ex: required approval \
            or unresolved blocking issue, will reject the transition). Returns the issue's full detail with its \
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
            Assign an unassigned issue to yourself. Use this to pick up work before acting on it. Fails with a \
            conflict if the issue is already assigned to someone else (re-claiming your own issue is a no-op). \
            Returns the issue's full detail with you as the assignee.""")
    public IssueDetail claimIssue(
            @McpToolParam(required = true, description = "The issue key to claim. ex: \"PROJ-123\".") String issueKey) {
        McpActor.requireWriteScope();

        issueParticipantUseCase.claim(IssueIdentifier.ofIssueKey(issueKey), McpActor.currentMemberId());

        return getDetail(issueKey);
    }

    @McpTool(name = "assign_issue", description = """
            Assign an issue to a specific member by their member id (use get_my_work / get_issue to find ids, or \
            claim_issue to assign it to yourself). This is a forceful assignment that replaces any current \
            assignee. The new assignee is automatically removed from the issue's reviewer list. Returns the \
            issue's full detail with the new assignee.""")
    public IssueDetail assignIssue(
            @McpToolParam(required = true, description = "The issue key. ex: \"PROJ-123\".") String issueKey,
            @McpToolParam(required = true, description = "The member id to assign the issue to.") Long memberId) {
        McpActor.requireWriteScope();

        issueParticipantUseCase.assign(IssueIdentifier.ofIssueKey(issueKey), memberId, McpActor.currentMemberId());

        return getDetail(issueKey);
    }

    @McpTool(name = "add_comment", description = """
            Add a comment to an issue. Use this to leave progress notes, ask questions, or hand off work. \
            mentionedUsernames notifies those members (pass usernames without the @). parentCommentId replies to \
            an existing comment (one level of nesting only). Returns the new comment id.""")
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
            // Do not chain the cause: the MCP runtime surfaces the most-specific cause's message to the
            // agent, and the raw parse message ("Text '...' could not be parsed") is not actionable.
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
