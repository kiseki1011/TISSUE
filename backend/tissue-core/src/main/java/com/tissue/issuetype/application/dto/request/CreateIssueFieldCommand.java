package com.tissue.issuetype.application.dto.request;

import com.tissue.global.vo.Name;
import com.tissue.issuetype.domain.enums.IssueFieldType;
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
