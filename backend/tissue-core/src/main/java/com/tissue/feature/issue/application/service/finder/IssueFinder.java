package com.tissue.feature.issue.application.service.finder;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.exception.IssueNotFoundException;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFinder {

    private final IssueQueryRepository issueQueryRepository;

    public Issue getWithProjectBy(String workspaceKey, String issueKey) {
        return issueQueryRepository
                .findWithProjectByKeys(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));
    }

    public Issue getWithFieldValuesBy(String workspaceKey, String issueKey) {
        return issueQueryRepository
                .findWithFieldValuesByKeys(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));
    }

    public Issue getDeletedWithProjectBy(String workspaceKey, String issueKey) {
        return issueQueryRepository
                .findDeletedWithProjectByKeys(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));
    }

    public List<Issue> getAllBy(Collection<String> issueKeys, String workspaceKey) {
        return issueQueryRepository.findByKeyInAndWorkspaceKey(issueKeys, workspaceKey);
    }

    public List<Issue> getAllBySprint(Sprint sprint) {
        return issueQueryRepository.findAllBySprint(sprint);
    }

    public List<Issue> getIncompleteIssuesBySprint(Sprint sprint) {
        return issueQueryRepository.findIncompleteIssuesBySprint(sprint, StateCategory.terminalCategories());
    }

    public List<String> getIncompleteIssueKeysBySprint(Sprint sprint) {
        return issueQueryRepository.findIncompleteIssueKeysBySprint(sprint, StateCategory.terminalCategories());
    }
}
