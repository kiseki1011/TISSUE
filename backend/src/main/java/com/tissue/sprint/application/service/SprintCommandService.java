package com.tissue.sprint.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.issue.domain.Issue;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
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
import com.tissue.sprint.application.service.finder.SprintFinder;
import com.tissue.sprint.application.service.validator.SprintValidator;
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
    private final SprintValidator sprintValidator;
    private final SprintCommandRepository sprintRepository;
    private final ProjectAuthorizationService projectAuthService;
    private final CurrentMemberProvider currentMemberProvider;

    @Override
    public SprintCommandResult createSprint(CreateSprintCommand cmd) {
        Long currentUserId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireProjectMember(cmd.workspaceKey(), cmd.projectKey(), currentUserId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());

        Sprint sprint = Sprint.create(project, cmd.title(), cmd.goal());
        sprintRepository.save(sprint);

        // TODO: SprintCreatedEvent

        return SprintCommandResult.from(sprint);
    }

    @Override
    public SprintCommandResult addIssues(AddSprintIssuesCommand cmd) {
        Long currentUserId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireProjectMember(cmd.workspaceKey(), cmd.projectKey(), currentUserId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Sprint sprint = sprintFinder.findBy(cmd.sprintId(), project);
        List<Issue> issues = issueFinder.findAllBy(cmd.issueKeys(), cmd.workspaceKey());

        sprintValidator.ensureSprintNotClosed(sprint);

        if (issues.isEmpty()) {
            return SprintCommandResult.from(sprint);
        }

        for (Issue issue : issues) {
            sprintValidator.ensureIssueInSprintProject(issue, project);
            issue.setSprint(sprint);
        }

        // TODO: SprintIssuesAddedEvent

        return SprintCommandResult.from(sprint);
    }

    @Override
    public SprintCommandResult updateSprint(UpdateSprintCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Sprint sprint = sprintFinder.findBy(cmd.sprintId(), project);

        Long currentUserId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireSprintEditPermission(cmd.workspaceKey(), cmd.projectKey(), sprint, currentUserId);

        Patchers.apply(cmd.title(), sprint::updateTitle);
        Patchers.apply(cmd.goal(), sprint::updateGoal);
        Patchers.apply(cmd.dueAt(), sprint::updateDueAt);
        Patchers.apply(cmd.startedAt(), sprint::updateStartedAt);

        // TODO: SprintUpdatedEvent

        return SprintCommandResult.from(sprint);
    }

    @Override
    public SprintCommandResult start(StartSprintCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Sprint sprint = sprintFinder.findBy(cmd.sprintId(), project);

        Long currentUserId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireSprintEditPermission(cmd.workspaceKey(), cmd.projectKey(), sprint, currentUserId);

        sprintValidator.ensureSprintNotClosed(sprint);
        sprintValidator.ensureNoActiveSprint(project);

        sprint.start(cmd.dueAt());

        // TODO: SprintStartedEvent

        return SprintCommandResult.from(sprint);
    }

    @Override
    public SprintCommandResult complete(CompleteSprintCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Sprint sprint = sprintFinder.findBy(cmd.sprintId(), project);

        Long currentUserId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireSprintEditPermission(cmd.workspaceKey(), cmd.projectKey(), sprint, currentUserId);

        List<String> incompleteIssueKeys = issueFinder.findIncompleteIssueKeysBySprint(sprint);

        if (sprint.isCompleted()) {
            return SprintCommandResult.from(sprint);
        }

        sprintValidator.ensureAllIssuesCompleted(incompleteIssueKeys, sprint);

        sprint.complete();

        // TODO: SprintCompletedEvent

        return SprintCommandResult.from(sprint);
    }

    @Override
    public SprintCommandResult migrateIssues(MigrateSprintIssuesCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Sprint originalSprint = sprintFinder.findBy(cmd.originalSprintId(), project);
        Sprint newSprint = sprintFinder.findBy(cmd.newSprintId(), project);

        Long currentUserId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireSprintEditPermission(
                cmd.workspaceKey(), cmd.projectKey(), originalSprint, currentUserId);
        projectAuthService.requireSprintEditPermission(cmd.workspaceKey(), cmd.projectKey(), newSprint, currentUserId);

        sprintValidator.ensureSprintNotClosed(originalSprint);
        sprintValidator.ensureSprintNotClosed(newSprint);

        List<Issue> issues = issueFinder.findIncompleteIssuesBySprint(originalSprint);

        if (issues.isEmpty()) {
            return SprintCommandResult.from(originalSprint);
        }

        for (Issue issue : issues) {
            issue.setSprint(newSprint);
        }

        // TODO: SprintIssuesMigratedEvent

        return SprintCommandResult.from(originalSprint);
    }

    @Override
    public SprintCommandResult removeIssues(RemoveSprintIssuesCommand cmd) {
        Long currentUserId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.requireProjectMember(cmd.workspaceKey(), cmd.projectKey(), currentUserId);

        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Sprint sprint = sprintFinder.findBy(cmd.sprintId(), project);

        sprintValidator.ensureSprintNotClosed(sprint);

        List<Issue> issues = issueFinder.findAllBy(cmd.issueKeys(), cmd.workspaceKey());

        for (Issue issue : issues) {
            sprintValidator.ensureIssueInSprintProject(issue, project);
            issue.clearSprint();
        }

        // TODO: SprintIssuesRemovedEvent

        return SprintCommandResult.from(sprint);
    }
}
