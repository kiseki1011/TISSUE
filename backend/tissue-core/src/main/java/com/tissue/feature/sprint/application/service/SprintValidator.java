package com.tissue.feature.sprint.application.service;

import static com.tissue.feature.sprint.domain.exception.SprintErrorCode.SPRINT_ALREADY_CLOSED;
import static com.tissue.feature.sprint.domain.exception.SprintErrorCode.SPRINT_ISSUE_PROJECT_MISMATCH;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.exception.ActiveSprintAlreadyExistsException;
import com.tissue.feature.sprint.domain.exception.IncompleteSprintIssuesFoundException;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SprintValidator {

    private final SprintFinder sprintFinder;

    public void ensureIssueInSprintProject(Issue issue, Project project) {
        if (!Objects.equals(issue.getProjectKey(), project.getKey())) {
            throw new BadRequestException(SPRINT_ISSUE_PROJECT_MISMATCH);
        }
    }

    public void ensureAllIssuesCompleted(List<String> incompleteIssueKeys, Sprint sprint) {
        if (!incompleteIssueKeys.isEmpty()) {
            throw new IncompleteSprintIssuesFoundException(incompleteIssueKeys, sprint.getProjectKey(), sprint.getId());
        }
    }

    public void ensureSprintNotClosed(Sprint sprint) {
        if (sprint.isCompleted()) {
            throw new BadRequestException(SPRINT_ALREADY_CLOSED);
        }
    }

    public void ensureNoActiveSprint(Project project) {
        sprintFinder.getActiveOptional(project).ifPresent(activeSprint -> {
            throw new ActiveSprintAlreadyExistsException(
                    project.getKey(), activeSprint.getId(), activeSprint.getTitle());
        });
    }
}
