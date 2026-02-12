package com.tissue.feature.issue.application.service.finder;

import static com.tissue.feature.workflow.domain.enums.StateCategory.COMPLETED;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.exception.IssueNotFoundException;
import com.tissue.feature.sprint.domain.Sprint;
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

    public List<Issue> getAllBy(Collection<String> issueKeys, String workspaceKey) {
        return issueQueryRepository.findByKeyInAndWorkspaceKey(issueKeys, workspaceKey);
    }

    public List<Issue> getIncompleteIssuesBySprint(Sprint sprint) {
        return issueQueryRepository.findIncompleteIssuesBySprint(sprint, COMPLETED);
    }

    public List<String> getIncompleteIssueKeysBySprint(Sprint sprint) {
        return issueQueryRepository.findIncompleteIssueKeysBySprint(sprint, COMPLETED);
    }
}
