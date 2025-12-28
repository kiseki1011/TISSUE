package com.tissue.sprint.domain.exception;

import static com.tissue.global.exception.ContextKeys.*;
import static com.tissue.sprint.domain.exception.SprintErrorCode.*;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.global.exception.base.ResourceNotFoundException;
import com.tissue.issue.domain.Issue;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.enums.SprintStatus;
import java.time.Instant;
import java.util.List;

public class SprintExceptions {

    private SprintExceptions() {}

    public static ResourceNotFoundException notFound(Long sprintId, String projectKey) {
        return new ResourceNotFoundException(SPRINT_NOT_FOUND)
                .addContext(SPRINT_ID, sprintId)
                .addContext(PROJECT_KEY, projectKey);
    }

    public static ResourceNotFoundException notFound(Long sprintId, Project project) {
        return new ResourceNotFoundException(SPRINT_NOT_FOUND)
                .addContext(SPRINT_ID, sprintId)
                .addContext(PROJECT_KEY, project.getKey())
                .addContext(WORKSPACE_KEY, project.getWorkspaceKey());
    }

    public static ResourceConflictException activeSprintAlreadyExists(
            String projectKey, Sprint activeSprint) {
        return new ResourceConflictException(ACTIVE_SPRINT_ALREADY_EXISTS)
                .addContext(PROJECT_KEY, projectKey)
                .addContext("activeSprintId", activeSprint.getId())
                .addContext("activeSprintTitle", activeSprint.getTitle());
    }

    public static BadRequestException incompleteIssuesFound(List<String> issueKeys, Sprint sprint) {
        return new BadRequestException(INCOMPLETE_SPRINT_ISSUES_FOUND)
                .addContext("incompleteIssueKeys", issueKeys)
                .addContext(SPRINT_ID, sprint.getId())
                .addContext(PROJECT_KEY, sprint.getProjectKey());
    }

    public static BadRequestException sprintClosed(Sprint sprint) {
        return new BadRequestException(SPRINT_ALREADY_CLOSED)
                .addContext(SPRINT_ID, sprint.getId())
                .addContext(PROJECT_KEY, sprint.getProjectKey())
                .addContext(WORKSPACE_KEY, sprint.getWorkspaceKey());
    }

    public static BadRequestException issueProjectMismatch(Issue issue, String sprintProjectKey) {
        return new BadRequestException(SPRINT_ISSUE_PROJECT_MISMATCH)
                .addContext(ISSUE_KEY, issue.getKey())
                .addContext("issueProjectKey", issue.getProjectKey())
                .addContext("sprintProjectKey", sprintProjectKey);
    }

    public static BadRequestException invalidPeriod(Instant start, Instant end) {
        return new BadRequestException(INVALID_SPRINT_PERIOD)
                .addContext("startDate", start)
                .addContext("endDate", end);
    }

    public static BadRequestException invalidStatusTransition(
            SprintStatus currentStatus,
            SprintStatus requiredCurrentStatus,
            SprintStatus targetStatus) {
        return new BadRequestException(INVALID_SPRINT_STATUS_TRANSITION)
                .addContext("currentStatus", currentStatus)
                .addContext("requiredCurrentStatus", requiredCurrentStatus)
                .addContext("targetStatus", targetStatus);
    }
}
