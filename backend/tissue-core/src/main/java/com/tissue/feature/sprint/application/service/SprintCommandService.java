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
import com.tissue.feature.sprint.domain.SprintFields;
import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.support.util.Patchers;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    public SprintCommandResult createSprint(ProjectIdentifier pid, CreateSprintCommand cmd, Long actorMemberId) {
        Project project = projectFinder.getWithLockBy(pid.workspaceKey(), pid.projectKey());
        ProjectMember actor = projectMemberFinder.getBy(project, actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Sprint sprint = Sprint.create(project, cmd.title(), cmd.goal());
        sprintRepository.save(sprint);

        eventPublisher.publishSprintCreated(sprint, actor);

        return SprintCommandResult.from(sprint);
    }

    @Override
    public void addIssues(String workspaceKey, Long sprintId, List<String> issueKeys, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProject(workspaceKey, sprintId);
        String projectKey = sprint.getProjectKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);

        List<Issue> issues = issueFinder.getAllBy(issueKeys, workspaceKey);

        sprintValidator.ensureSprintNotClosed(sprint);

        if (issues.isEmpty()) {
            return;
        }

        for (Issue issue : issues) {
            sprintValidator.ensureIssueInSprintProject(issue, sprint.getProject());
            issue.setSprint(sprint);
        }

        eventPublisher.publishIssuesAdded(sprint, issueKeys, actor);
    }

    @Override
    public void updateSprint(String workspaceKey, Long sprintId, UpdateSprintCommand cmd, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProject(workspaceKey, sprintId);

        ProjectMember actor = projectMemberFinder.getBy(sprint.getProject(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Map<String, FieldChange> changes = new HashMap<>();

        Patchers.applyWithLog(cmd.title(), sprint::getTitle, sprint::updateTitle, SprintFields.TITLE, changes);
        Patchers.applyWithLog(cmd.goal(), sprint::getGoal, sprint::updateGoal, SprintFields.GOAL, changes);
        Patchers.applyWithLog(
                cmd.startedAt(), sprint::getStartedAt, sprint::updateStartedAt, SprintFields.STARTED_AT, changes);
        Patchers.applyWithLog(cmd.dueAt(), sprint::getDueAt, sprint::updateDueAt, SprintFields.DUE_AT, changes);

        if (!changes.isEmpty()) {
            eventPublisher.publishSprintUpdated(sprint, changes, actor);
        }
    }

    @Override
    public void start(String workspaceKey, Long sprintId, Instant dueAt, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProject(workspaceKey, sprintId);

        ProjectMember actor = projectMemberFinder.getBy(sprint.getProject(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        sprintValidator.ensureSprintNotClosed(sprint);
        sprintValidator.ensureNoActiveSprint(sprint.getProject());

        sprint.start(dueAt);

        eventPublisher.publishSprintStarted(sprint, actor);
    }

    @Override
    public void complete(String workspaceKey, Long sprintId, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProject(workspaceKey, sprintId);

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
    public void migrateIssues(String workspaceKey, Long sprintId, MigrateSprintIssuesCommand cmd, Long actorMemberId) {
        Sprint sourceSprint = sprintFinder.getWithProject(workspaceKey, sprintId);

        ProjectMember actor = projectMemberFinder.getBy(sourceSprint.getProject(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        Sprint targetSprint = sprintFinder.getWithProject(workspaceKey, cmd.targetSprintId());

        sprintValidator.ensureSprintNotClosed(sourceSprint);
        sprintValidator.ensureSprintNotClosed(targetSprint);

        List<Issue> issues = issueFinder.getIncompleteIssuesBySprint(sourceSprint);

        if (issues.isEmpty()) {
            return;
        }

        for (Issue issue : issues) {
            issue.setSprint(targetSprint);
        }

        eventPublisher.publishIssuesAdded(
                targetSprint, issues.stream().map(Issue::getKey).toList(), actor);
    }

    @Override
    public void removeIssues(String workspaceKey, Long sprintId, List<String> issueKeys, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProject(workspaceKey, sprintId);
        String projectKey = sprint.getProjectKey();

        ProjectMember actor = projectMemberFinder.getWithWorkspaceMember(workspaceKey, projectKey, actorMemberId);

        sprintValidator.ensureSprintNotClosed(sprint);

        List<Issue> issues = issueFinder.getAllBy(issueKeys, workspaceKey);

        for (Issue issue : issues) {
            sprintValidator.ensureIssueInSprintProject(issue, sprint.getProject());
            issue.clearSprint();
        }

        eventPublisher.publishIssuesRemoved(sprint, issueKeys, actor);
    }

    @Override
    public void cancelSprint(String workspaceKey, Long sprintId, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProject(workspaceKey, sprintId);

        ProjectMember actor = projectMemberFinder.getBy(sprint.getProject(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        sprint.cancel();

        List<Issue> issues = issueFinder.getAllBySprint(sprint);
        for (Issue issue : issues) {
            issue.clearSprint();
        }

        eventPublisher.publishSprintCancelled(sprint, actor);
    }

    @Override
    public void deleteSprint(String workspaceKey, Long sprintId, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProject(workspaceKey, sprintId);

        ProjectMember actor = projectMemberFinder.getBy(sprint.getProject(), actorMemberId);
        projectAuthorizationService.requireProjectManager(actor);

        sprintValidator.ensureSprintCancelled(sprint);

        sprint.softDelete();

        eventPublisher.publishSprintDeleted(sprint, actor);
    }
}
