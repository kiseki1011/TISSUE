package com.tissue.issuetype.application.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Name;
import com.tissue.issue.domain.enums.IssueHierarchy;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateIssueTypeCommand(
        String workspaceKey,
        String projectKey,
        Name name,
        @Nullable String description,
        ColorType color,
        IssueHierarchy issueHierarchy,
        Long workflowId) {}
