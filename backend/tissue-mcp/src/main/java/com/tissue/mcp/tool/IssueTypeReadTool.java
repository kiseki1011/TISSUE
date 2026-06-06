package com.tissue.mcp.tool;

import com.tissue.feature.issuetype.application.dto.response.IssueTypeDetail;
import com.tissue.feature.issuetype.application.port.usecase.IssueTypeQueryUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueTypeReadTool {

    private final IssueTypeQueryUseCase issueTypeQueryUseCase;

    @McpTool(name = "get_issue_types", description = """
            List every issue type with its full schema, so you know what to pass to create_issue. \
            Each entry has an id (use it as the issueTypeId when creating an issue), name, description, \
            hierarchy (EPIC, STANDARD, SUBTASK, MICROTASK), workflow, and a fields array. Each field has an \
            id (the key in the create_issue customFields map), name, type (TEXT, INTEGER, DECIMAL, TIMESTAMP, \
            DATE, BOOLEAN, SELECT_OPTION, PERCENTAGE, CHECKLIST), required (you must supply a value when true), \
            and, for SELECT_OPTION/CHECKLIST types, an options array whose ids are the allowed values. \
            Call this before create_issue to pick a type and satisfy its required fields.""")
    public List<IssueTypeDetail> getIssueTypes() {
        return issueTypeQueryUseCase.getIssueTypeDetails(McpActor.currentMemberId());
    }
}
