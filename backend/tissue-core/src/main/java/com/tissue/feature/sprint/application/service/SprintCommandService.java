package com.tissue.feature.sprint.application.service;

import com.tissue.feature.issue.application.service.finder.IssueFinder;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.feature.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.feature.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.feature.sprint.application.dto.response.SprintCommandResult;
import com.tissue.feature.sprint.application.port.repository.SprintCommandRepository;
import com.tissue.feature.sprint.application.port.usecase.SprintCommandUseCase;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.util.Patchers;
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
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueFinder issueFinder;
    private final SprintCommandRepository sprintRepository;
    private final SprintValidator sprintValidator;
    private final ProjectAuthorizationService projectAuthorizationService;
    private final SprintEventPublisher eventPublisher;

    @Override
    public SprintCommandResult createSprint(
            ProjectIdentifier projectIdentifier, CreateSprintCommand cmd, Long actorMemberId) {
        Project project = projectFinder.getBy(projectIdentifier.workspaceKey(), projectIdentifier.projectKey());
        ProjectMember actor = projectMemberFinder.getBy(project, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Sprint sprint = Sprint.create(project, cmd.title(), cmd.goal());
        sprintRepository.save(sprint);

        // TODO: SprintCreatedEvent

        return SprintCommandResult.from(sprint);
    }

    @Override
    public void addIssues(
            ProjectIdentifier projectIdentifier, Long sprintId, List<String> issueKeys, Long actorMemberId) {
        Project project = projectFinder.getBy(projectIdentifier.workspaceKey(), projectIdentifier.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        Sprint sprint = sprintFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), sprintId);

        List<Issue> issues = issueFinder.getAllBy(issueKeys, projectIdentifier.workspaceKey());

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
    public void updateSprint(
            ProjectIdentifier projectIdentifier, Long sprintId, UpdateSprintCommand cmd, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), sprintId);

        ProjectMember actor = projectMemberFinder.getBy(sprint.getProject(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Patchers.apply(cmd.title(), sprint::updateTitle);
        Patchers.apply(cmd.goal(), sprint::updateGoal);
        Patchers.apply(cmd.dueAt(), sprint::updateDueAt);
        Patchers.apply(cmd.startedAt(), sprint::updateStartedAt);

        // TODO: SprintUpdatedEvent
    }

    @Override
    public void start(ProjectIdentifier projectIdentifier, Long sprintId, Instant dueAt, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), sprintId);

        ProjectMember actor = projectMemberFinder.getBy(sprint.getProject(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        sprintValidator.ensureSprintNotClosed(sprint);
        sprintValidator.ensureNoActiveSprint(sprint.getProject());

        sprint.start(dueAt);

        eventPublisher.publishSprintStarted(sprint, actor);
    }

    @Override
    public void complete(ProjectIdentifier projectIdentifier, Long sprintId, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), sprintId);

        ProjectMember actor = projectMemberFinder.getBy(sprint.getProject(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        List<String> incompleteIssueKeys = issueFinder.getIncompleteIssueKeysBySprint(sprint);

        if (sprint.isCompleted()) {
            return;
        }

        sprintValidator.ensureAllIssuesCompleted(incompleteIssueKeys, sprint);

        sprint.complete();

        eventPublisher.publishSprintCompleted(sprint, actor);
    }

    @Override
    public void migrateIssues(
            ProjectIdentifier projectIdentifier, Long sprintId, MigrateSprintIssuesCommand cmd, Long actorMemberId) {
        Sprint sourceSprint = sprintFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), sprintId);

        ProjectMember actor = projectMemberFinder.getBy(sourceSprint.getProject(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Sprint targetSprint = sprintFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), cmd.targetSprintId());

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
    public void removeIssues(
            ProjectIdentifier projectIdentifier, Long sprintId, List<String> issueKeys, Long actorMemberId) {
        Project project = projectFinder.getBy(projectIdentifier.workspaceKey(), projectIdentifier.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        Sprint sprint = sprintFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), sprintId);

        sprintValidator.ensureSprintNotClosed(sprint);

        List<Issue> issues = issueFinder.getAllBy(issueKeys, projectIdentifier.workspaceKey());

        // TODO: Do i need optimization?
        for (Issue issue : issues) {
            sprintValidator.ensureIssueInSprintProject(issue, sprint.getProject());
            issue.clearSprint();
        }

        // TODO: SprintIssuesRemovedEvent
    }
}
