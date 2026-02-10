package com.tissue.feature.issuetype.application.dto.request;

import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.vo.Name;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateIssueFieldCommand(
        Name name,
        @Nullable String description,
        IssueFieldType issueFieldType,
        Boolean required,
        List<Name> initialOptions) {}
