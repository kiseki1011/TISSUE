package com.tissue.issue.application.service.finder;

import static com.tissue.workflow.domain.enums.StateCategory.COMPLETED;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issue.domain.Issue;
import com.tissue.issue.domain.exception.IssueExceptions;
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

    // TODO: find -> get
    public Issue findBy(String issueKey, Project project) {
        return issueQueryRepo
                .findByKeyAndProject(issueKey, project)
                .orElseThrow(() -> IssueExceptions.notFound(project.getWorkspaceKey(), issueKey));
    }

    // TODO: getIncludingSoftDeleted
    //  - is there a better name?
    //  - a pagination api
    //  - is used by ADMIN(WorkspaceRole, ProjectRole, SystemRole) to see all issues for a project
    //  including soft-deleted issues

    // TODO: find -> get
    public List<Issue> findAllBy(Collection<String> issueKeys, String workspaceKey) {
        return issueQueryRepo.findByKeyInAndWorkspaceKey(issueKeys, workspaceKey);
    }

    // TODO: find -> get
    public List<Issue> findIncompleteIssuesBySprint(Sprint sprint) {
        return issueQueryRepo.findIncompleteIssuesBySprint(sprint, COMPLETED);
    }

    // TODO: find -> get
    public List<String> findIncompleteIssueKeysBySprint(Sprint sprint) {
        return issueQueryRepo.findIncompleteIssueKeysBySprint(sprint, COMPLETED);
    }
}
