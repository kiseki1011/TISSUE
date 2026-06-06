package com.tissue.mcp.tool;

import com.tissue.feature.issue.application.dto.response.IssueDetail;
import com.tissue.feature.issue.application.dto.response.TransitionDetail;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.shared.dto.IssueIdentifier;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueReadTool {

    private final IssueQueryUseCase issueQueryUseCase;

    @McpTool(name = "get_issue", description = """
            Fetch the full detail of a single issue. Returns its common fields (title, description, priority, \
            story point, schedule, current workflow state, assignee, reviewers, progress) and the issue type's \
            custom fields with their values. Read an issue with this before acting on it (commenting, \
            transitioning, assigning).""")
    public IssueDetail getIssue(
            @McpToolParam(required = true, description = "The issue key, ex: \"PROJ-123\".") String issueKey) {
        return issueQueryUseCase.getDetail(IssueIdentifier.ofIssueKey(issueKey), McpActor.currentMemberId());
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
}
