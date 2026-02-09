package com.tissue.sprint.application.service;

import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.exception.ActiveSprintAlreadyExistsException;
import com.tissue.sprint.domain.exception.IncompleteSprintIssuesFoundException;
import com.tissue.sprint.domain.exception.SprintClosedException;
import com.tissue.sprint.domain.exception.SprintIssueProjectMismatchException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintValidator {

    private final SprintFinder sprintFinder;

    public void ensureIssueInSprintProject(Issue issue, Project project) {
        if (!issue.getProjectKey().equals(project.getKey())) {
            throw new SprintIssueProjectMismatchException(issue, project.getKey());
        }
    }

    public void ensureAllIssuesCompleted(List<String> incompleteIssueKeys, Sprint sprint) {
        if (!incompleteIssueKeys.isEmpty()) {
            throw new IncompleteSprintIssuesFoundException(incompleteIssueKeys, sprint);
        }
    }

    public void ensureSprintNotClosed(Sprint sprint) {
        if (sprint.isCompleted()) {
            throw new SprintClosedException(sprint.getProjectKey(), sprint.getId());
        }
    }

    public void ensureNoActiveSprint(Project project) {
        sprintFinder.getActiveOptional(project).ifPresent(activeSprint -> {
            throw new ActiveSprintAlreadyExistsException(project.getKey(), activeSprint);
        });
    }
}
