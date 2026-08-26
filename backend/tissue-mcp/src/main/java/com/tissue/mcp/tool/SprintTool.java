package com.tissue.mcp.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintSummary;
import com.tissue.feature.sprint.application.port.usecase.SprintCommandUseCase;
import com.tissue.feature.sprint.application.port.usecase.SprintQueryUseCase;
import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.shared.dto.PageResponse;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/**
 * Reading a project's sprints, and moving issues in and out of them.
 *
 * <p>Creating, starting, completing and cancelling a sprint are deliberately not exposed: where a sprint
 * begins and ends is the team's planning decision, and an agent that could close one would be deciding
 * when everyone else's work is done.
 */
@Component
@RequiredArgsConstructor
public class SprintTool {

    private static final int PAGE_SIZE = 25;

    private final SprintQueryUseCase sprintQueryUseCase;
    private final SprintCommandUseCase sprintCommandUseCase;

    @McpTool(name = "list_sprints", description = """
            List a project's sprints, newest first, with the sprintId that create_issue and \
            add_issues_to_sprint need. Each entry carries its title, goal, status and dates. Filter by \
            status to answer a specific question: ["ACTIVE"] is the sprint being worked on right now, \
            ["PLANNING"] is what is lined up next. If hasNext is true, pass the next page number as the \
            page argument.""")
    public PageResponse<SprintSummary> listSprints(
            @McpToolParam(required = true, description = "The project key, ex: \"PROJ\".") String projectKey,
            @McpToolParam(required = false, description = """
                            Limit to sprints in these states. Any of "PLANNING" (not started yet), \
                            "ACTIVE" (running now), "COMPLETED" (finished), "CANCELLED" (abandoned). \
                            Omit for every sprint.""") @Nullable List<String> statuses,
            @McpToolParam(required = false, description = "Zero-based page number. Omit for the first page.") @Nullable
                    Integer page) {
        // sorted rather than left to the database: an unordered page would repeat and drop sprints as the
        // agent walks pages, and the sprint number is the only stable ordering the entity carries
        PageRequest newestFirst =
                PageRequest.of(page == null ? 0 : page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "sprintNumber"));

        return PageResponse.from(sprintQueryUseCase.getProjectSprints(
                ProjectIdentifier.ofProjectKey(projectKey),
                parseStatuses(statuses),
                newestFirst,
                McpActor.currentMemberId()));
    }

    @McpTool(name = "get_sprint", description = """
            Fetch one sprint: its title, goal, status, and when it started, is due and completed. Read \
            the goal before deciding what to work on - it says what this sprint is for, which the issues \
            alone do not. Pass includeIssueKeys to also get the keys of every issue in it.""")
    public SprintView getSprint(
            @McpToolParam(required = true, description = "The sprint id, from list_sprints.") Long sprintId,
            @McpToolParam(
                            required = false,
                            description = "Also return the key of every issue in the sprint. Defaults to false.")
                    @Nullable
                    Boolean includeIssueKeys) {
        Long actorMemberId = McpActor.currentMemberId();
        SprintDetail detail = sprintQueryUseCase.getSprintDetail(sprintId, actorMemberId);

        List<String> issueKeys = Boolean.TRUE.equals(includeIssueKeys)
                ? sprintQueryUseCase.getSprintIssueKeys(sprintId, actorMemberId).issueKeys()
                : null;

        return new SprintView(detail, issueKeys);
    }

    @McpTool(name = "add_issues_to_sprint", description = """
            Schedule issues into a sprint, moving them out of the backlog. An issue belongs to one sprint \
            at a time, so this moves an issue that is already in another one. Use it to put work you just \
            created where the team will see it.""")
    public void addIssuesToSprint(
            @McpToolParam(required = true, description = "The sprint id, from list_sprints.") Long sprintId,
            @McpToolParam(required = true, description = "Issue keys to schedule, ex: [\"PROJ-1\", \"PROJ-2\"].")
                    List<String> issueKeys) {
        McpActor.requireWriteScope();

        sprintCommandUseCase.addIssues(sprintId, issueKeys, McpActor.currentMemberId());
    }

    @McpTool(name = "remove_issues_from_sprint", description = """
            Take issues out of a sprint, returning them to the backlog. The issues themselves are \
            untouched - only their scheduling changes.""")
    public void removeIssuesFromSprint(
            @McpToolParam(required = true, description = "The sprint id, from list_sprints.") Long sprintId,
            @McpToolParam(required = true, description = "Issue keys to unschedule, ex: [\"PROJ-1\", \"PROJ-2\"].")
                    List<String> issueKeys) {
        McpActor.requireWriteScope();

        sprintCommandUseCase.removeIssues(sprintId, issueKeys, McpActor.currentMemberId());
    }

    private static @Nullable Set<SprintStatus> parseStatuses(@Nullable List<String> names) {
        if (names == null || names.isEmpty()) {
            return null;
        }
        Set<SprintStatus> statuses = EnumSet.noneOf(SprintStatus.class);
        for (String name : names) {
            try {
                statuses.add(SprintStatus.valueOf(name.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Unknown sprint status: \"" + name
                        + "\". Valid statuses are: PLANNING, ACTIVE, COMPLETED, CANCELLED.");
            }
        }
        return statuses;
    }

    /**
     * A sprint plus, when asked for, what is in it. The keys are null rather than empty when they were not
     * requested, so an agent cannot read "not loaded" as "the sprint is empty".
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SprintView(SprintDetail sprint, @Nullable List<String> issueKeys) {}
}
