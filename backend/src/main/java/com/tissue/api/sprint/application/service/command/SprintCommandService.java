package com.tissue.api.sprint.application.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.util.Patchers;
import com.tissue.api.issue.application.service.finder.IssueFinder;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.domain.Project;
import com.tissue.api.sprint.application.dto.request.AddSprintIssuesCommand;
import com.tissue.api.sprint.application.dto.request.CompleteSprintCommand;
import com.tissue.api.sprint.application.dto.request.CreateSprintCommand;
import com.tissue.api.sprint.application.dto.request.MigrateSprintIssuesCommand;
import com.tissue.api.sprint.application.dto.request.RemoveSprintIssuesCommand;
import com.tissue.api.sprint.application.dto.request.StartSprintCommand;
import com.tissue.api.sprint.application.dto.request.UpdateSprintCommand;
import com.tissue.api.sprint.application.dto.response.SprintCommandResult;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.domain.service.SprintValidator;
import com.tissue.api.sprint.infrastructure.repository.SprintRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintCommandService {

	private final SprintFinder sprintFinder;
	private final ProjectFinder projectFinder;
	private final IssueFinder issueFinder;
	private final SprintValidator sprintValidator;
	private final SprintRepository sprintRepository;

	@Transactional
	public SprintCommandResult createSprint(CreateSprintCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());

		Sprint sprint = Sprint.create(
			project,
			cmd.title(),
			cmd.goal()
		);

		sprintRepository.save(sprint);

		// TODO: SprintCreatedEvent
		//  - 대상: 해당 프로젝트 인원 전원

		return SprintCommandResult.from(sprint);
	}

	@Transactional
	public SprintCommandResult addIssues(AddSprintIssuesCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Sprint sprint = sprintFinder.findBy(cmd.sprintId(), project);
		List<Issue> issues = issueFinder.findAllBy(cmd.issueKeys(), cmd.workspaceKey());

		sprintValidator.ensureSprintNotClosed(sprint, project);

		if (issues.isEmpty()) {
			return SprintCommandResult.from(sprint);
		}

		for (Issue issue : issues) {
			sprintValidator.ensureIssueInSprintProject(issue, project);
			issue.setSprint(sprint);
		}

		// TODO: SprintIssuesAddedEvent
		//  - 대상: 해당 이슈들의 관련자 전원

		return SprintCommandResult.from(sprint);
	}

	@Transactional
	public SprintCommandResult updateSprint(UpdateSprintCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Sprint sprint = sprintFinder.findBy(cmd.sprintId(), project);

		Patchers.apply(cmd.title(), sprint::updateTitle);
		Patchers.apply(cmd.goal(), sprint::updateGoal);
		Patchers.apply(cmd.dueAt(), sprint::updateDueAt);
		Patchers.apply(cmd.startedAt(), sprint::updateStartedAt);

		// TODO: SprintUpdatedEvent
		//  - 대상: 해당 프로젝트 인원 전원

		return SprintCommandResult.from(sprint);
	}

	@Transactional
	public SprintCommandResult start(StartSprintCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Sprint sprint = sprintFinder.findBy(cmd.sprintId(), project);

		sprintValidator.ensureSprintNotClosed(sprint, project);
		sprintValidator.ensureNoActiveSprint(project);

		sprint.start(cmd.startedAt(), cmd.dueAt());

		// TODO: SprintStartedEvent
		//  - 대상: 해당 프로젝트 인원 전원

		return SprintCommandResult.from(sprint);
	}

	@Transactional
	public SprintCommandResult complete(CompleteSprintCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Sprint sprint = sprintFinder.findBy(cmd.sprintId(), project);

		List<String> incompleteIssueKeys = issueFinder.findIncompleteIssueKeysBySprint(sprint);

		if (sprint.isCompleted()) {
			return SprintCommandResult.from(sprint);
		}

		sprintValidator.ensureAllIssuesCompleted(incompleteIssueKeys, sprint, project);

		sprint.complete();

		// TODO: SprintCompletedEvent
		//  - 대상: 해당 프로젝트 인원 전원

		return SprintCommandResult.from(sprint);
	}

	@Transactional
	public SprintCommandResult migrateIssues(MigrateSprintIssuesCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Sprint originalSprint = sprintFinder.findBy(cmd.originalSprintId(), project);
		Sprint newSprint = sprintFinder.findBy(cmd.newSprintId(), project);

		sprintValidator.ensureSprintNotClosed(originalSprint, project);
		sprintValidator.ensureSprintNotClosed(newSprint, project);

		List<Issue> issues = issueFinder.findIncompleteIssuesBySprint(originalSprint);

		if (issues.isEmpty()) {
			return SprintCommandResult.from(originalSprint);
		}

		for (Issue issue : issues) {
			issue.setSprint(newSprint);
		}

		// TODO: SprintIssuesMigratedEvent
		//  - 대상: 해당 이슈들의 관련자 전원

		return SprintCommandResult.from(originalSprint);
	}

	@Transactional
	public SprintCommandResult removeIssues(RemoveSprintIssuesCommand cmd) {

		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Sprint sprint = sprintFinder.findBy(cmd.sprintId(), project);

		sprintValidator.ensureSprintNotClosed(sprint, project);

		List<Issue> issues = issueFinder.findAllBy(cmd.issueKeys(), cmd.workspaceKey());

		for (Issue issue : issues) {
			sprintValidator.ensureIssueInSprintProject(issue, project);
			issue.clearSprint();
		}

		// TODO: SprintIssuesRemovedEvent
		//  - 대상: 해당 이슈들의 관련자 전원

		return SprintCommandResult.from(sprint);
	}
}
