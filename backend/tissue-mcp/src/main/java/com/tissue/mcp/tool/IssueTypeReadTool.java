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
            List every issue type with its full schema, so you can pick one for create_issue. Each entry has an \
            id (pass as issueTypeId to create_issue), name, description, hierarchy, workflow, and a fields \
            array. Each field has an id (the string key to use in the create_issue customFields map), name, \
            type, required (you must supply a value when true), and - for SELECT_OPTION and CHECKLIST fields - \
            an options array whose ids are the allowed values. Call this before create_issue to choose a type \
            and satisfy its required fields.""")
    public List<IssueTypeDetail> getIssueTypes() {
        return issueTypeQueryUseCase.getIssueTypeDetails(McpActor.currentMemberId());
    }
}
