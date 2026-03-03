package com.tissue.feature.issuetype.application.dto.request;

import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.vo.Name;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CreateIssueFieldCommand(
        Name name,
        @Nullable String description,
        IssueFieldType issueFieldType,
        boolean required,
        List<Name> initialOptions,
        int position) {}
