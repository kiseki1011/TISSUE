package com.tissue.mcp.tool;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.port.usecase.IssueFullTextSearchUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueListQueryUseCase;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.PageResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueListTool {

    private static final int PAGE_SIZE = 50;
    private static final int SEARCH_PAGE_SIZE = 25;

    private final IssueListQueryUseCase issueListQueryUseCase;
    private final IssueFullTextSearchUseCase issueFtsUseCase;

    @McpTool(name = "get_my_work", description = """
            List the issues in a specific project assigned to you that are not yet done. \
            Start here to find what to work on. Returns a page of issue summaries, highest priority \
            first. If more remain, pass the returned nextCursor back as the cursor argument.""")
    public CursorPage<IssueSummary> getMyWork(
            @McpToolParam(required = true, description = "The project key, ex: \"PROJ\".") String projectKey,
            @McpToolParam(
                            required = false,
                            description = "Opaque cursor from a previous page. Omit for the first page.")
                    @Nullable
                    String cursor) {
        return issueListQueryUseCase.getMyWork(
                ProjectIdentifier.ofProjectKey(projectKey), McpActor.currentMemberId(), cursor, PAGE_SIZE);
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

    @McpTool(name = "search_issues", description = """
            Find issues by keyword, ranked by how well they match. The keyword is matched against the \
            issue's key, title and body. Use this to locate an issue you only know by subject ("the \
            login timeout bug") rather than by key.

            Searches every project you belong to unless you name one. If hasNext is true, pass the next \
            page number as the page argument.""")
    public PageResponse<IssueSummary> searchIssues(
            @McpToolParam(required = true, description = "Words to search for.") String keyword,
            @McpToolParam(
                            required = false,
                            description = "Limit the search to one project, ex: \"PROJ\". "
                                    + "Omit to search every project you belong to.")
                    @Nullable
                    String projectKey,
            @McpToolParam(required = false, description = """
                            Limit to issues in these workflow state categories. Any of "INITIAL" (not \
                            started), "ACTIVE" (in progress), "COMPLETED" (done), "ABORTED" (cancelled). \
                            Pass ["INITIAL", "ACTIVE"] for open work only. Omit for every state.""") @Nullable List<String> stateCategories,
            @McpToolParam(required = false, description = "Zero-based page number. Omit for the first page.") @Nullable
                    Integer page) {
        IssueSearchCondition condition = new IssueSearchCondition(
                null,
                parseStateCategories(stateCategories),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                keyword);

        int pageNumber = page == null ? 0 : page;
        Long actorMemberId = McpActor.currentMemberId();

        Page<IssueSummary> result = projectKey == null
                ? issueFtsUseCase.ftsAllRanked(condition, pageNumber, SEARCH_PAGE_SIZE, actorMemberId)
                : issueFtsUseCase.ftsByProjectRanked(
                        ProjectIdentifier.ofProjectKey(projectKey),
                        condition,
                        pageNumber,
                        SEARCH_PAGE_SIZE,
                        actorMemberId);

        return PageResponse.from(result);
    }

    @Nullable
    private static Set<StateCategory> parseStateCategories(@Nullable List<String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        Set<StateCategory> categories = new LinkedHashSet<>();
        for (String name : names) {
            try {
                categories.add(StateCategory.valueOf(name.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown state category: \"" + name
                        + "\". Valid categories are: INITIAL, ACTIVE, COMPLETED, ABORTED.");
            }
        }
        return categories;
    }
}
