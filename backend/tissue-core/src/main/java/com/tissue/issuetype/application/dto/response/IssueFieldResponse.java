package com.tissue.issuetype.application.dto.response;

import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;

public record IssueFieldResponse(String projectKey, Long issueTypeId, Long issueFieldId) {

    public static IssueFieldResponse from(IssueField issueField, IssueType issueType) {
        return new IssueFieldResponse(issueType.getProjectKey(), issueType.getId(), issueField.getId());
    }
}
