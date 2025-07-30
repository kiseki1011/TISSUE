package com.tissue.api.sprint.application.service.command;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.base.application.finder.IssueFinder;
import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.sprint.domain.event.SprintCompletedEvent;
import com.tissue.api.sprint.domain.event.SprintStartedEvent;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;
import com.tissue.api.sprint.infrastructure.repository.SprintRepository;
import com.tissue.api.sprint.presentation.dto.request.AddSprintIssuesRequest;
import com.tissue.api.sprint.presentation.dto.request.CreateSprintRequest;
import com.tissue.api.sprint.presentation.dto.request.RemoveSprintIssueRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintRequest;
import com.tissue.api.sprint.presentation.dto.request.UpdateSprintStatusRequest;
import com.tissue.api.sprint.presentation.dto.response.SprintResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintCommandService {

	private final SprintFinder sprintFinder;
	private final SprintRepository sprintRepository;
	private final WorkspaceFinder workspaceFinder;
	private final IssueFinder issueFinder;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public SprintResponse createSprint(
		String workspaceCode,
		CreateSprintRequest request
	) {
		Workspace workspace = workspaceFinder.findWorkspace(workspaceCode);

		Sprint sprint = Sprint.builder()
			.title(request.title())
			.goal(request.goal())
			.plannedStartDate(request.plannedStartDate())
			.plannedEndDate(request.plannedEndDate())
			.workspace(workspace)
			.build();

		Sprint savedSprint = sprintRepository.save(sprint);
		return SprintResponse.from(savedSprint);
	}

	@Transactional
	public SprintResponse addIssues(
		String workspaceCode,
		String sprintKey,
		AddSprintIssuesRequest request
	) {
		Sprint sprint = sprintFinder.findSprint(sprintKey, workspaceCode);

		List<Issue> issues = issueFinder.findIssues(request.issueKeys(), workspaceCode);

		for (Issue issue : issues) {
			sprint.addIssue(issue);
		}

		return SprintResponse.from(sprint);
	}

	@Transactional
	public SprintResponse updateSprint(
		String workspaceCode,
		String sprintKey,
		UpdateSprintRequest request
	) {
		Sprint sprint = sprintFinder.findSprint(sprintKey, workspaceCode);

		sprint.updateTitle(request.title() != null ? request.title() : sprint.getTitle());
		sprint.updateGoal(request.goal() != null ? request.goal() : sprint.getGoal());

		LocalDateTime startDate =
			request.plannedStartDate() != null ? request.plannedStartDate() : sprint.getPlannedStartDate();
		LocalDateTime endDate =
			request.plannedEndDate() != null ? request.plannedEndDate() : sprint.getPlannedEndDate();

		if (request.plannedStartDate() != null || request.plannedEndDate() != null) {
			sprint.updateDates(startDate, endDate);
		}

		return SprintResponse.from(sprint);
	}

	@Transactional
	public SprintResponse updateSprintStatus(
		String workspaceCode,
		String sprintKey,
		UpdateSprintStatusRequest request,
		Long currentWorkspaceMemberId
	) {
		Sprint sprint = sprintFinder.findSprint(sprintKey, workspaceCode);

		sprint.updateStatus(request.newStatus());

		if (sprint.getStatus() == SprintStatus.ACTIVE) {
			eventPublisher.publishEvent(SprintStartedEvent.createEvent(sprint, currentWorkspaceMemberId));
		} else if (sprint.getStatus() == SprintStatus.COMPLETED) {
			eventPublisher.publishEvent(SprintCompletedEvent.createEvent(sprint, currentWorkspaceMemberId));
		}

		return SprintResponse.from(sprint);
	}

	@Transactional
	public SprintResponse removeIssue(
		String workspaceCode,
		String sprintKey,
		RemoveSprintIssueRequest request
	) {
		Issue issue = issueFinder.findIssueInSprint(sprintKey, request.issueKey(), workspaceCode);
		Sprint sprint = sprintFinder.findSprint(sprintKey, workspaceCode);

		sprint.removeIssue(issue);

		return SprintResponse.from(sprint);
	}
}
