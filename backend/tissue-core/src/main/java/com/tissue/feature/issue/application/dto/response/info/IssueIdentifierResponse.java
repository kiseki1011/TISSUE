package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import org.jspecify.annotations.Nullable;

public record IssueIdentifierResponse(
        @Nullable String issueKey,
        @Nullable IssueTypeInfo issueType,
        @Nullable StateInfo currentState) {

    @LLMGenerated(llmInvolvement = LLMInvolvement.ASSISTED)
    public static IssueIdentifierResponse from(Issue issue) {
        return new IssueIdentifierResponse(
                issue.getKey(), IssueTypeInfo.from(issue.getIssueType()), StateInfo.from(issue.getCurrentState()));
    }

    public static IssueIdentifierResponse asNull() {
        return new IssueIdentifierResponse(null, null, null);
    }
}
