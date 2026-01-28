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
        projectAuthService.requireProjectMember(actorContext);

        Project project = projectFinder.getModifiableBy(actorContext.projectId());

        Sprint sprint = Sprint.create(project, cmd.title(), cmd.goal());
        sprintRepository.save(sprint);

        // TODO: SprintCreatedEvent

        return SprintCommandResult.from(sprint);
    }

    @Override
    public void addIssues(AddSprintIssuesCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        projectAuthService.requireProjectMember(actorContext);

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Sprint sprint = sprintFinder.getBy(cmd.sprintId(), project);
        List<Issue> issues = issueFinder.getAllBy(cmd.issueKeys(), actorContext.workspaceKey());

        sprintValidator.ensureSprintNotClosed(sprint);

        if (issues.isEmpty()) {
            return;
        }

        // TODO: 루프문 성능 문제가 있을까?
        //  (어차피 하나의 스프린트 내에 존재할 이슈의 수가 제한적이라 크게 걱정은 안되지만)
        for (Issue issue : issues) {
            sprintValidator.ensureIssueInSprintProject(issue, project);
            issue.setSprint(sprint);
        }

        // TODO: SprintIssuesAddedEvent
    }

    @Override
    public void updateSprint(UpdateSprintCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Sprint sprint = sprintFinder.getBy(cmd.sprintId(), project);

        projectAuthService.requireSprintEditPermission(actorContext, sprint);

        Patchers.apply(cmd.title(), sprint::updateTitle);
        Patchers.apply(cmd.goal(), sprint::updateGoal);
        Patchers.apply(cmd.dueAt(), sprint::updateDueAt);
        Patchers.apply(cmd.startedAt(), sprint::updateStartedAt);

        // TODO: SprintUpdatedEvent
    }

    @Override
    public void start(StartSprintCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Sprint sprint = sprintFinder.getBy(cmd.sprintId(), project);

        projectAuthService.requireSprintEditPermission(actorContext, sprint);

        sprintValidator.ensureSprintNotClosed(sprint);
        sprintValidator.ensureNoActiveSprint(project);

        sprint.start(cmd.dueAt());

        eventPublisher.publishSprintStarted(sprint, actorContext);
    }

    @Override
    public void complete(CompleteSprintCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Sprint sprint = sprintFinder.getBy(cmd.sprintId(), project);

        projectAuthService.requireSprintEditPermission(actorContext, sprint);

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
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Sprint originalSprint = sprintFinder.getBy(cmd.originalSprintId(), project);
        Sprint newSprint = sprintFinder.getBy(cmd.newSprintId(), project);

        projectAuthService.requireSprintEditPermission(actorContext, originalSprint);
        // TODO: 굳이 newSprint에 대한 권한이 필요할까?
        // projectAuthService.requireSprintEditPermission(actorContext, newSprint);

        sprintValidator.ensureSprintNotClosed(originalSprint);
        sprintValidator.ensureSprintNotClosed(newSprint);

        List<Issue> issues = issueFinder.getIncompleteIssuesBySprint(originalSprint);

        if (issues.isEmpty()) {
            return;
        }

        // TODO: 루프문 성능 문제가 있을까?
        //  (어차피 하나의 스프린트 내에 존재할 이슈의 수가 제한적이라 크게 걱정은 안되지만)
        for (Issue issue : issues) {
            issue.setSprint(newSprint);
        }

        // TODO: SprintIssuesMigratedEvent
    }

    @Override
    public void removeIssues(RemoveSprintIssuesCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Sprint sprint = sprintFinder.getBy(cmd.sprintId(), project);

        // TODO: 프로젝트 멤버가 스프린트의 이슈를 제거하는걸 허용하는게 맞겟지?
        //  이슈를 스프린트에 추가하는건 열려있음. 만약 실수로 이슈를 추가한 경우 제거하고 싶을 수 있으니깐
        //  그냥 MEMBER 권한도 허용하려는데.
        projectAuthService.requireProjectMember(actorContext);
        sprintValidator.ensureSprintNotClosed(sprint);

        List<Issue> issues = issueFinder.getAllBy(cmd.issueKeys(), actorContext.workspaceKey());

        // TODO: 루프문 성능 문제가 있을까?
        //  (어차피 하나의 스프린트 내에 존재할 이슈의 수가 제한적이라 크게 걱정은 안되지만)
        for (Issue issue : issues) {
            sprintValidator.ensureIssueInSprintProject(issue, project);
            issue.clearSprint();
        }

        // TODO: SprintIssuesRemovedEvent
    }
}
