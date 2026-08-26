package com.tissue.mcp.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.usecase.ActivityLogQueryUseCase;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.IssueIdentifier;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

/**
 * The audit trail of an issue, rendered as sentences.
 *
 * <p>Sprint activity is deliberately not exposed. Its query matches on the sprint resource itself, and
 * only two events are ever recorded against one - started and completed - which {@code get_sprint}
 * already reports as dates.
 */
@Component
@RequiredArgsConstructor
public class ActivityTool {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final ActivityLogQueryUseCase activityLogQueryUseCase;

    @McpTool(name = "get_issue_activity", description = """
            Read what has happened to an issue, newest first, one sentence per event: who moved it \
            between states, who took it, when a branch or pull request was linked, what a review said. \
            get_issue shows where the issue stands now - this shows how it got there.

            Reach for it when the current state alone does not explain something: an issue that came back \
            from done, a field that is not what you expect, work that stalled. A field edit also lists \
            what changed, as "field: before -> after".

            Comments are not here. They are the discussion rather than the record of it, and \
            get_issue with the comments section returns them. If hasNext is true, pass nextCursor back as \
            the cursor argument to read further back in time.""")
    public CursorPage<ActivityEntry> getIssueActivity(
            @McpToolParam(required = true, description = "The issue key, ex: \"PROJ-123\".") String issueKey,
            @McpToolParam(required = false, description = "How many events to return, at most 100. Defaults to 20.")
                    @Nullable
                    Integer limit,
            @McpToolParam(
                            required = false,
                            description = "Opaque cursor from a previous page's nextCursor. Omit to start "
                                    + "from the most recent event.")
                    @Nullable
                    String cursor) {
        CursorPage<ActivityLogResponse> page = activityLogQueryUseCase.getIssueActivities(
                IssueIdentifier.ofIssueKey(issueKey), McpActor.currentMemberId(), cursor, clamp(limit));

        List<ActivityEntry> entries =
                page.content().stream().map(ActivityEntry::from).toList();

        return new CursorPage<>(entries, page.nextCursor(), page.hasNext());
    }

    /**
     * Bounded rather than passed through: the limit reaches a keyset query directly, and an agent that
     * asked for a whole issue's history in one call would spend its context on it.
     */
    private static int clamp(@Nullable Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    /**
     * One event as a reader takes it in: when it happened and what happened, with the raw type kept so a
     * caller can pick out a kind of event without matching on English. Changes are absent unless the
     * sentence left something unsaid.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ActivityEntry(
            Instant occurredAt,
            ActivityType type,
            String summary,
            @Nullable List<String> changes) {

        static ActivityEntry from(ActivityLogResponse log) {
            List<String> changes = ActivitySentence.changeLines(log);

            return new ActivityEntry(
                    log.occurredAt(), log.type(), ActivitySentence.summarize(log), changes.isEmpty() ? null : changes);
        }
    }
}
