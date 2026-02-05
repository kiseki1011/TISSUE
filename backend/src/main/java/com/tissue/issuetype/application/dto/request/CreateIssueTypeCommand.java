package com.tissue.issuetype.application.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.global.vo.Name;
import com.tissue.issue.domain.enums.IssueHierarchy;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateIssueTypeCommand(
        Name name, @Nullable String description, ColorType color, IssueHierarchy issueHierarchy, Long workflowId) {}
