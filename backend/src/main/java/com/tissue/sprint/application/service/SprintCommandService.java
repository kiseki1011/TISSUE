package com.tissue.sprint.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.sprint.application.dto.response.SprintCommandResult;
import com.tissue.sprint.application.port.in.SprintCommandUseCase;
import com.tissue.sprint.application.port.out.SprintCommandRepository;
import com.tissue.sprint.domain.Sprint;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class SprintCommandService implements SprintCommandUseCase {

    private final SprintFinder sprintFinder;
    private final ProjectFinder projectFinder;
    private final IssueFinder issueFinder;
    private final SprintCommandRepository sprintRepository;
    private final SprintValidator sprintValidator;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final SprintEventPublisher eventPublisher;

    @Override
    public SprintCommandResult createSprint(CreateSprintCommand cmd, ProjectMemberContext actorContext) {
        projectAuthorizationService.requireProjectManager(actorContext);

        Project project = projectFinder.getBy(actorContext.workspaceKey(), actorContext.projectKey());

        Sprint sprint = Sprint.create(project, cmd.title(), cmd.goal());
        sprintRepository.save(sprint);

        // TODO: SprintCreatedEvent

        return SprintCommandResult.from(sprint);
    }

    @Override
    public void addIssues(Long sprintId, List<String> issueKeys, ProjectMemberContext actorContext) {
        Sprint sprint = sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), sprintId);
        List<Issue> issues = issueFinder.getAllBy(issueKeys, actorContext.workspaceKey());

        sprintValidator.ensureSprintNotClosed(sprint);

        if (issues.isEmpty()) {
            return;
        }

        // TODO: Do i need to optimize this loop?
        //  Maybe, if there are tons of issues inside a single sprint.
        for (Issue issue : issues) {
            sprintValidator.ensureIssueInSprintProject(issue, sprint.getProject());
            issue.setSprint(sprint);
        }

        // TODO: SprintIssuesAddedEvent
    }

    @Override
    public void updateSprint(Long sprintId, UpdateSprintCommand cmd, ProjectMemberContext actorContext) {
        projectAuthorizationService.requireProjectManager(actorContext);
        Sprint sprint = sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), sprintId);

        Patchers.apply(cmd.title(), sprint::updateTitle);
        Patchers.apply(cmd.goal(), sprint::updateGoal);
        Patchers.apply(cmd.dueAt(), sprint::updateDueAt);
        Patchers.apply(cmd.startedAt(), sprint::updateStartedAt);

        // TODO: SprintUpdatedEvent
    }

    @Override
    public void start(Long sprintId, Instant dueAt, ProjectMemberContext actorContext) {
        projectAuthorizationService.requireProjectManager(actorContext);

        Sprint sprint = sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), sprintId);

        sprintValidator.ensureSprintNotClosed(sprint);
        sprintValidator.ensureNoActiveSprint(sprint.getProject());

        sprint.start(dueAt);

        eventPublisher.publishSprintStarted(sprint, actorContext);
    }

    @Override
    public void complete(Long sprintId, ProjectMemberContext actorContext) {
        projectAuthorizationService.requireProjectManager(actorContext);

        Sprint sprint = sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), sprintId);

        List<String> incompleteIssueKeys = issueFinder.getIncompleteIssueKeysBySprint(sprint);

        if (sprint.isCompleted()) {
            return;
        }

        sprintValidator.ensureAllIssuesCompleted(incompleteIssueKeys, sprint);

        sprint.complete();

        eventPublisher.publishSprintCompleted(sprint, actorContext);
    }

    @Override
    public void migrateIssues(Long sprintId, MigrateSprintIssuesCommand cmd, ProjectMemberContext actorContext) {
        projectAuthorizationService.requireProjectManager(actorContext);

        Sprint sourceSprint =
                sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), sprintId);
        Sprint targetSprint = sprintFinder.getWithProjectBy(
                actorContext.workspaceKey(), actorContext.projectKey(), cmd.targetSprintId());

        sprintValidator.ensureSprintNotClosed(sourceSprint);
        sprintValidator.ensureSprintNotClosed(targetSprint);

        List<Issue> issues = issueFinder.getIncompleteIssuesBySprint(sourceSprint);

        if (issues.isEmpty()) {
            return;
        }

        // TODO: Do i need optimization?
        for (Issue issue : issues) {
            issue.setSprint(targetSprint);
        }

        // TODO: SprintIssuesMigratedEvent
    }

    @Override
    public void removeIssues(Long sprintId, List<String> issueKeys, ProjectMemberContext actorContext) {
        Sprint sprint = sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), sprintId);

        sprintValidator.ensureSprintNotClosed(sprint);

        List<Issue> issues = issueFinder.getAllBy(issueKeys, actorContext.workspaceKey());

        // TODO: Do i need optimization?
        for (Issue issue : issues) {
            sprintValidator.ensureIssueInSprintProject(issue, sprint.getProject());
            issue.clearSprint();
        }

        // TODO: SprintIssuesRemovedEvent
    }
}
