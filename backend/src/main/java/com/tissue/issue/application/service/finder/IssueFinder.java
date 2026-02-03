package com.tissue.issue.application.service.finder;

import static com.tissue.workflow.domain.enums.StateCategory.COMPLETED;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.IssueNotFoundException;
import com.tissue.sprint.domain.Sprint;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFinder {

    private final IssueQueryRepository issueQueryRepo;

    public Issue getBy(String workspaceKey, String issueKey) {
        return issueQueryRepo
                .findByKeyWithProject(workspaceKey, issueKey)
                .orElseThrow(() -> new IssueNotFoundException(workspaceKey, issueKey));
    }

    public List<Issue> getAllBy(Collection<String> issueKeys, String workspaceKey) {
        return issueQueryRepo.findByKeyInAndWorkspaceKey(issueKeys, workspaceKey);
    }

    public List<Issue> getIncompleteIssuesBySprint(Sprint sprint) {
        return issueQueryRepo.findIncompleteIssuesBySprint(sprint, COMPLETED);
    }

    public List<String> getIncompleteIssueKeysBySprint(Sprint sprint) {
        return issueQueryRepo.findIncompleteIssueKeysBySprint(sprint, COMPLETED);
    }
}
