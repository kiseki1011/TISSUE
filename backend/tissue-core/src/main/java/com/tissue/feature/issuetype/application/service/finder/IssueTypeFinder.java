package com.tissue.feature.issuetype.application.service.finder;

import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.exception.IssueTypeNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueTypeFinder {

    private final IssueTypeRepository issueTypeRepository;

    public IssueType getWithProjectBy(String workspaceKey, Long issueTypeId) {
        return issueTypeRepository
                .findWithProjectByWorkspaceKeyAndId(workspaceKey, issueTypeId)
                .orElseThrow(() -> new IssueTypeNotFoundException(issueTypeId));
    }

    public IssueType getWithProjectAndWorkflowBy(String workspaceKey, String projectKey, Long issueTypeId) {
        return issueTypeRepository
                .findWithProjectAndWorkflowByWorkspaceKeyAndProjectKeyAndId(workspaceKey, projectKey, issueTypeId)
                .orElseThrow(() -> new IssueTypeNotFoundException(projectKey, issueTypeId));
    }
}
