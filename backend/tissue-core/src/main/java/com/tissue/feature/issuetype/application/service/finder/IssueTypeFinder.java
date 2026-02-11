package com.tissue.feature.issuetype.application.service.finder;

import com.tissue.feature.issuetype.application.port.repository.IssueTypeQueryRepository;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.exception.IssueTypeNotFoundException;
import com.tissue.feature.project.domain.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueTypeFinder {

    private final IssueTypeQueryRepository issueTypeQueryRepository;

    public IssueType getBy(Long issueTypeId, Project project) {
        return issueTypeQueryRepository
                .findByIdAndProject(issueTypeId, project)
                .orElseThrow(() -> new IssueTypeNotFoundException(issueTypeId, project));
    }

    public IssueType getWithProjectBy(String workspaceKey, String projectKey, Long issueTypeId) {
        return issueTypeQueryRepository
                .findWithProjectByWorkspaceKeyAndProjectKeyAndId(workspaceKey, projectKey, issueTypeId)
                .orElseThrow(() -> new IssueTypeNotFoundException(projectKey, issueTypeId));
    }
}
