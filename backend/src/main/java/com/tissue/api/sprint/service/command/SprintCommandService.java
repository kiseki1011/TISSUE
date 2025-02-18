package com.tissue.api.sprint.service.command;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.repository.SprintRepository;
import com.tissue.api.sprint.presentation.dto.request.CreateSprintRequest;
import com.tissue.api.sprint.presentation.dto.response.CreateSprintResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.service.query.WorkspaceQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintCommandService {

	private final WorkspaceQueryService workspaceQueryService;
	private final SprintRepository sprintRepository;

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
}
