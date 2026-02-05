package com.tissue.sprint.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.sprint.application.dto.request.AddSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.CompleteSprintCommand;
import com.tissue.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.RemoveSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.StartSprintCommand;
import com.tissue.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.sprint.application.dto.response.SprintCommandResult;
import com.tissue.sprint.application.port.in.SprintCommandUseCase;
import com.tissue.sprint.application.port.out.SprintCommandRepository;
import com.tissue.sprint.domain.Sprint;
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
    private final ProjectAuthorizationService projectAuthService;
    private final SprintEventPublisher eventPublisher;

    @Override
    public SprintCommandResult createSprint(CreateSprintCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Project project = projectFinder.getBy(actorContext.workspaceKey(), actorContext.projectKey());

        Sprint sprint = Sprint.create(project, cmd.title(), cmd.goal());
        sprintRepository.save(sprint);

        // TODO: SprintCreatedEvent

        return SprintCommandResult.from(sprint);
    }

    @Override
    public void addIssues(AddSprintIssuesCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Sprint sprint =
                sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), cmd.sprintId());
        List<Issue> issues = issueFinder.getAllBy(cmd.issueKeys(), actorContext.workspaceKey());

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
    public void updateSprint(UpdateSprintCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Sprint sprint =
                sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), cmd.sprintId());

        // TODO: projectEditPermission?
        // projectAuthService.requireSprintEditPermission(actorContext, sprint);

        Patchers.apply(cmd.title(), sprint::updateTitle);
        Patchers.apply(cmd.goal(), sprint::updateGoal);
        Patchers.apply(cmd.dueAt(), sprint::updateDueAt);
        Patchers.apply(cmd.startedAt(), sprint::updateStartedAt);

        // TODO: SprintUpdatedEvent
    }

    @Override
    public void start(StartSprintCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Sprint sprint =
                sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), cmd.sprintId());

        // TODO: projectEditPermission?
        // projectAuthService.requireSprintEditPermission(actorContext, sprint);

        sprintValidator.ensureSprintNotClosed(sprint);
        sprintValidator.ensureNoActiveSprint(sprint.getProject());

        sprint.start(cmd.dueAt());

        eventPublisher.publishSprintStarted(sprint, actorContext);
    }

    @Override
    public void complete(CompleteSprintCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Sprint sprint =
                sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), cmd.sprintId());

        // TODO: projectEditPermission?
        // projectAuthService.requireSprintEditPermission(actorContext, sprint);

        List<String> incompleteIssueKeys = issueFinder.getIncompleteIssueKeysBySprint(sprint);

        if (sprint.isCompleted()) {
            return;
        }

        sprintValidator.ensureAllIssuesCompleted(incompleteIssueKeys, sprint);

        sprint.complete();

        eventPublisher.publishSprintCompleted(sprint, actorContext);
    }

    @Override
    public void migrateIssues(MigrateSprintIssuesCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        // TODO: projectEditPermission?

        Sprint originalSprint = sprintFinder.getWithProjectBy(
                actorContext.workspaceKey(), actorContext.projectKey(), cmd.originalSprintId());
        Sprint newSprint = sprintFinder.getWithProjectBy(
                actorContext.workspaceKey(), actorContext.projectKey(), cmd.newSprintId());

        sprintValidator.ensureSprintNotClosed(originalSprint);
        sprintValidator.ensureSprintNotClosed(newSprint);

        List<Issue> issues = issueFinder.getIncompleteIssuesBySprint(originalSprint);

        if (issues.isEmpty()) {
            return;
        }

        // TODO: Do i need optimization?
        for (Issue issue : issues) {
            issue.setSprint(newSprint);
        }

        // TODO: SprintIssuesMigratedEvent
    }

    @Override
    public void removeIssues(RemoveSprintIssuesCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Sprint sprint =
                sprintFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), cmd.sprintId());

        // TODO: projectEditPermission?

        sprintValidator.ensureSprintNotClosed(sprint);

        List<Issue> issues = issueFinder.getAllBy(cmd.issueKeys(), actorContext.workspaceKey());

        // TODO: Do i need optimization?
        for (Issue issue : issues) {
            sprintValidator.ensureIssueInSprintProject(issue, sprint.getProject());
            issue.clearSprint();
        }

        // TODO: SprintIssuesRemovedEvent
    }
}
