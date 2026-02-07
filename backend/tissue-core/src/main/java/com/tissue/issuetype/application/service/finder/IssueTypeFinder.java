package com.tissue.issuetype.application.service.finder;

import com.tissue.issuetype.application.port.out.IssueTypeQueryRepository;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.exception.IssueTypeNotFoundException;
import com.tissue.project.domain.Project;
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
