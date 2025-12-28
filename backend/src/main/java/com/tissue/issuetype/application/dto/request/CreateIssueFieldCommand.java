package com.tissue.issuetype.application.dto.request;

import com.tissue.common.vo.Name;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import java.util.List;
import lombok.Builder;

@Builder
public record CreateIssueFieldCommand(
        String workspaceKey,
        String projectKey,
        Long issueTypeId,
        Name name,
        String description,
        IssueFieldType issueFieldType,
        Boolean required,
        List<Name> initialOptions) {}
