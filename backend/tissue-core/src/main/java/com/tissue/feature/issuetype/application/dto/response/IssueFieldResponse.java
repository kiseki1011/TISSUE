package com.tissue.feature.issuetype.application.dto.response;

import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;

public record IssueFieldResponse(String projectKey, Long issueTypeId, Long issueFieldId) {

    public static IssueFieldResponse from(IssueField issueField, IssueType issueType) {
        return new IssueFieldResponse(issueType.getProjectKey(), issueType.getId(), issueField.getId());
    }
}
