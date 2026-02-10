package com.tissue.feature.issuetype.application.dto.request;

import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateIssueTypeCommand(
        Name name, @Nullable String description, ColorType color, IssueHierarchy issueHierarchy, Long workflowId) {}
