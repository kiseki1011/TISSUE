package com.tissue.issue.application.service.finder;

import static com.tissue.workflow.domain.enums.StateCategory.COMPLETED;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.IssueNotFoundException;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFinder {

    private final IssueQueryRepository issueQueryRepo;

    public Issue getBy(String issueKey, Project project) {
        return issueQueryRepo
                .findByKeyAndProject(issueKey, project)
                .orElseThrow(() -> new IssueNotFoundException(project.getWorkspaceKey(), issueKey));
    }

    // TODO: getIncludingSoftDeleted
    //  - is there a better name?
    //  - a pagination api
    //  - is used by ADMIN(WorkspaceRole, ProjectRole, SystemRole) to see all issues for a project
    //  including soft-deleted issues

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
