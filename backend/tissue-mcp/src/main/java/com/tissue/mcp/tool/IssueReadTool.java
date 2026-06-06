package com.tissue.mcp.tool;

import com.tissue.feature.issue.application.dto.response.IssueDetail;
import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import com.tissue.shared.dto.IssueIdentifier;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueReadTool {

    private final IssueQueryUseCase issueQueryUseCase;

    @McpTool(
            name = "get_issue",
            description = "Fetch the full detail of a single issue by its key (ex: \"PROJ-123\"). "
                    + "Returns the common fields (title, description, priority, story point, schedule, current "
                    + "workflow state, assignee, reviewers, progress) and the issue type's custom fields with their "
                    + "values. Can read a ticket with this before acting on it (commenting, transitioning, updating).")
    public IssueDetail getIssue(@McpToolParam(required = true, description = "The issue key") String issueKey) {
        return issueQueryUseCase.getDetail(IssueIdentifier.ofIssueKey(issueKey), McpActor.currentMemberId());
    }
}
