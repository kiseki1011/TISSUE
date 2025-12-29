package com.tissue.sprint.application.service.validator;

import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.Project;
import com.tissue.sprint.application.service.finder.SprintFinder;
import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.exception.SprintExceptions;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintValidator {

    private final SprintFinder sprintFinder;

    public void ensureIssueInSprintProject(Issue issue, Project project) {
        if (!issue.getProjectKey().equals(project.getKey())) {
            throw SprintExceptions.issueProjectMismatch(issue, project.getKey());
        }
    }

    public void ensureAllIssuesCompleted(List<String> incompleteIssueKeys, Sprint sprint) {
        if (!incompleteIssueKeys.isEmpty()) {
            throw SprintExceptions.incompleteIssuesFound(incompleteIssueKeys, sprint);
        }
    }

    public void ensureSprintNotClosed(Sprint sprint) {
        if (sprint.isCompleted()) {
            throw SprintExceptions.sprintClosed(sprint);
        }
    }

    public void ensureNoActiveSprint(Project project) {
        sprintFinder.findActiveBy(project).ifPresent(activeSprint -> {
            throw SprintExceptions.activeSprintAlreadyExists(project.getKey(), activeSprint);
        });
    }
}
