package com.tissue.feature.issuetype.application.service.finder;

import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.exception.IssueTypeNotFoundException;
import com.tissue.feature.project.domain.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueTypeFinder {

    private final IssueTypeRepository issueTypeRepository;

    public IssueType getBy(Long issueTypeId, Project project) {
        return issueTypeRepository
                .findByIdAndProject(issueTypeId, project)
                .orElseThrow(() -> new IssueTypeNotFoundException(project.getKey(), issueTypeId));
    }

    public IssueType getWithProjectBy(String workspaceKey, String projectKey, Long issueTypeId) {
        return issueTypeRepository
                .findWithProjectByWorkspaceKeyAndProjectKeyAndId(workspaceKey, projectKey, issueTypeId)
                .orElseThrow(() -> new IssueTypeNotFoundException(projectKey, issueTypeId));
    }

    public IssueType getWithProjectAndWorkflowBy(String workspaceKey, String projectKey, Long issueTypeId) {
        return issueTypeRepository
                .findWithProjectAndWorkflowByWorkspaceKeyAndProjectKeyAndId(workspaceKey, projectKey, issueTypeId)
                .orElseThrow(() -> new IssueTypeNotFoundException(projectKey, issueTypeId));
    }
}
