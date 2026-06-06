package com.tissue.mcp.tool;

import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.port.usecase.IssueListQueryUseCase;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.ProjectIdentifier;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueListTool {

    private static final int PAGE_SIZE = 50;

    private final IssueListQueryUseCase issueListQueryUseCase;

    @McpTool(name = "get_my_work", description = """
            List the issues assigned to you that are not yet done, across every project you belong to. \
            Start here to find what to work on. Returns a page of issue summaries, highest priority \
            first. If more remain, pass the returned nextCursor back as the cursor argument.""")
    public CursorPage<IssueSummary> getMyWork(
            @McpToolParam(
                            required = false,
                            description = "Opaque cursor from a previous page. Omit for the first page.")
                    @Nullable
                    String cursor) {
        return issueListQueryUseCase.getMyWork(McpActor.currentMemberId(), cursor, PAGE_SIZE);
    }

    @McpTool(name = "get_backlog", description = """
            List issues of a specific project, not yet added to any sprint and still in their initial state. \
            Returns a page of issue summaries, highest priority first. If more \
            remain, pass the returned nextCursor back as the cursor argument.""")
    public CursorPage<IssueSummary> getBacklog(
            @McpToolParam(required = true, description = "The project key, ex: \"PROJ\".") String projectKey,
            @McpToolParam(
                            required = false,
                            description = "Opaque cursor from a previous page. Omit for the first page.")
                    @Nullable
                    String cursor) {
        return issueListQueryUseCase.getBacklog(
                ProjectIdentifier.ofProjectKey(projectKey), McpActor.currentMemberId(), cursor, PAGE_SIZE);
    }

    @McpTool(name = "get_current_sprint_issues", description = """
            List the issues in a project's current active sprint. (What the project is working on right now.) \
            Returns an empty list when the project has no active sprint. Otherwise returns a page of issue summaries, \
            highest priority first. If more remain, pass the returned nextCursor back as the cursor argument.""")
    public CursorPage<IssueSummary> getCurrentSprintIssues(
            @McpToolParam(required = true, description = "The project key, ex: \"PROJ\".") String projectKey,
            @McpToolParam(
                            required = false,
                            description = "Opaque cursor from a previous page. Omit for the first page.")
                    @Nullable
                    String cursor) {
        return issueListQueryUseCase.getCurrentSprintIssues(
                ProjectIdentifier.ofProjectKey(projectKey), McpActor.currentMemberId(), cursor, PAGE_SIZE);
    }
}
