package com.tissue.feature.issuetype.application.dto.response;

import com.tissue.feature.issuetype.domain.IssueType;

public record IssueTypeResponse(String projectKey, Long issueTypeId) {

    public static IssueTypeResponse from(IssueType issueType) {
        return new IssueTypeResponse(issueType.getProjectKey(), issueType.getId());
    }
}
