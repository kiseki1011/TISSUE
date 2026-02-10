package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.issue.domain.Issue;
import org.jspecify.annotations.Nullable;

public record IssueIdentifierResponse(
        @Nullable String issueKey, @Nullable String issueTypeLabel) {

    public static IssueIdentifierResponse from(Issue issue) {
        return new IssueIdentifierResponse(issue.getKey(), issue.getIssueType().getName());
    }

    public static IssueIdentifierResponse asNull() {
        return new IssueIdentifierResponse(null, null);
    }
}
