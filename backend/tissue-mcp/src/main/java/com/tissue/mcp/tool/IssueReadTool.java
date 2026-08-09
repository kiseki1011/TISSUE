package com.tissue.mcp.tool;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tissue.feature.comment.application.dto.response.CommentDetailResponse;
import com.tissue.feature.issue.application.dto.request.IssueDetailSection;
import com.tissue.feature.issue.application.dto.response.IssueBranchView;
import com.tissue.feature.issue.application.dto.response.IssueCommonDetail;
import com.tissue.feature.issue.application.dto.response.IssueDetailView;
import com.tissue.feature.issue.application.dto.response.IssuePullRequestView;
import com.tissue.feature.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.feature.issue.application.dto.response.TransitionDetail;
import com.tissue.feature.issue.application.dto.response.info.CustomFieldValueInfo;
import com.tissue.feature.issue.application.dto.response.info.IssueIdentifierResponse;
import com.tissue.feature.issue.application.port.usecase.IssueDetailViewUseCase;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.shared.dto.IssueIdentifier;
import com.tissue.shared.dto.PageResponse;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueReadTool {

    /** Matches the issue detail API, so a comment page read here is the same page a person sees. */
    private static final int COMMENT_PAGE_SIZE = 20;

    private final IssueQueryUseCase issueQueryUseCase;
    private final IssueDetailViewUseCase issueDetailViewUseCase;

    @McpTool(name = "get_issue", description = """
            Fetch a single issue. Returns its common fields (title, description, priority, story point, \
            schedule, current workflow state, assignee, reviewers, progress) and the issue type's custom \
            fields with their values. Read an issue with this before acting on it (commenting, \
            transitioning, assigning).

            Pass `include` to also load the sections you need. Each one costs an extra query and adds to \
            the response, so ask only for what you will use; omitted sections are left out entirely.""")
    public IssueView getIssue(
            @McpToolParam(required = true, description = "The issue key, ex: \"PROJ-123\".") String issueKey,
            @McpToolParam(required = false, description = """
                            Extra sections to load. Any of:
                            - "comments": the discussion on the issue, newest page first. A review's \
                            feedback carries a reviewStatus saying which verdict it was submitted with.
                            - "relations": issues this one blocks, causes, duplicates or relates to, and \
                            the same in reverse.
                            - "hierarchy": the parent issue and the child issues.
                            - "vcs": linked branches and pull requests, with their current state.
                            Omit for the issue alone. Workflow transitions are not here - \
                            call list_available_transitions for those.""") @Nullable List<String> include) {
        Set<IssueDetailSection> sections = parseSections(include);

        IssueDetailView view = issueDetailViewUseCase.getDetailView(
                IssueIdentifier.ofIssueKey(issueKey), sections, COMMENT_PAGE_SIZE, McpActor.currentMemberId());

        return IssueView.of(view, sections);
    }

    @McpTool(name = "list_available_transitions", description = """
            List the workflow transitions available from an issue's current state. Each entry has a transitionId \
            (pass it to transition_issue), a displayLabel, canExecute (false when a guard blocks it), and \
            blockedReasons explaining what blocks it (ex: unresolved blocking issues, a missing approval, no \
            linked branch). Call this before transition_issue to pick a valid transition and learn why others \
            are blocked.""")
    public List<TransitionDetail> listAvailableTransitions(
            @McpToolParam(required = true, description = "The issue key, ex: \"PROJ-123\".") String issueKey) {
        return issueQueryUseCase.getAvailableTransitions(
                IssueIdentifier.ofIssueKey(issueKey), McpActor.currentMemberId());
    }

    /**
     * Transitions are deliberately not selectable here: {@code list_available_transitions} already returns
     * them with the guard reasons, and offering the same data twice only gives an agent a second way to ask
     * the same question.
     */
    private static Set<IssueDetailSection> parseSections(@Nullable List<String> include) {
        Set<IssueDetailSection> sections = EnumSet.noneOf(IssueDetailSection.class);
        if (include == null) {
            return sections;
        }
        for (String name : include) {
            sections.add(
                    switch (name.trim().toLowerCase(Locale.ROOT)) {
                        case "comments" -> IssueDetailSection.COMMENTS;
                        case "relations" -> IssueDetailSection.RELATIONS;
                        case "hierarchy" -> IssueDetailSection.HIERARCHY;
                        case "vcs" -> IssueDetailSection.VCS;
                        default ->
                            throw new IllegalArgumentException("Unknown include section: \"" + name
                                    + "\". Valid sections are: comments, relations, hierarchy, vcs.");
                    });
        }
        return sections;
    }

    /**
     * An issue as an agent reads it. A section that was not asked for is null and is dropped from the JSON,
     * so an agent that wanted only the issue body does not carry empty comment and relation objects around
     * in its context.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record IssueView(
            IssueCommonDetail common,
            List<CustomFieldValueInfo> customFields,
            @Nullable IssueIdentifierResponse parent,
            @Nullable List<IssueIdentifierResponse> children,
            @Nullable IssueRelationsDetail relations,
            @Nullable PageResponse<CommentDetailResponse> comments,
            @Nullable List<IssueBranchView> branches,
            @Nullable List<IssuePullRequestView> pullRequests) {

        static IssueView of(IssueDetailView view, Set<IssueDetailSection> sections) {
            boolean hierarchy = sections.contains(IssueDetailSection.HIERARCHY);
            boolean vcs = sections.contains(IssueDetailSection.VCS);

            // an issue with no parent comes back as a null-object rather than absent, which would serialize
            // as an empty parent and read as "there is one, unnamed"
            boolean hasParent = hierarchy && view.parent().issueKey() != null;

            return new IssueView(
                    view.common(),
                    view.customFields(),
                    hasParent ? view.parent() : null,
                    hierarchy ? view.children() : null,
                    sections.contains(IssueDetailSection.RELATIONS) ? view.relations() : null,
                    sections.contains(IssueDetailSection.COMMENTS) ? view.comments() : null,
                    vcs ? view.branches() : null,
                    vcs ? view.pullRequests() : null);
        }
    }
}
