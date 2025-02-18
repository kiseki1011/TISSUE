package com.tissue.api.sprint.service.command;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.query.IssueQueryService;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.repository.SprintRepository;
import com.tissue.api.sprint.presentation.dto.request.AddSprintIssuesRequest;
import com.tissue.api.sprint.presentation.dto.request.CreateSprintRequest;
import com.tissue.api.sprint.presentation.dto.response.AddSprintIssuesResponse;
import com.tissue.api.sprint.presentation.dto.response.CreateSprintResponse;
import com.tissue.api.sprint.service.query.SprintQueryService;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.service.query.WorkspaceQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintCommandService {

	private final SprintQueryService sprintQueryService;
	private final SprintRepository sprintRepository;
	private final WorkspaceQueryService workspaceQueryService;
	private final IssueQueryService issueQueryService;

	@Transactional
	public CreateSprintResponse createSprint(String workspaceCode, CreateSprintRequest request) {
		Workspace workspace = workspaceQueryService.findWorkspace(workspaceCode);

		Sprint sprint = Sprint.builder()
			.title(request.title())
			.goal(request.goal())
			.startDate(request.startDate())
			.endDate(request.endDate())
			.workspace(workspace)
			.build();

		Sprint savedSprint = sprintRepository.save(sprint);
		return CreateSprintResponse.from(savedSprint);
	}

	@Transactional
	public AddSprintIssuesResponse addIssues(
		String workspaceCode,
		String sprintKey,
		AddSprintIssuesRequest request
	) {
		Sprint sprint = sprintQueryService.findSprint(sprintKey, workspaceCode);

		List<Issue> issues = issueQueryService.findIssues(request.issueKeys(), workspaceCode);

		for (Issue issue : issues) {
			sprint.addIssue(issue);
		}

		return AddSprintIssuesResponse.of(sprint, request.issueKeys());
	}
}
