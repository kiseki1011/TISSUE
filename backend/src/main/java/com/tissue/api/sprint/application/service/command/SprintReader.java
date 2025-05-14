package com.tissue.api.sprint.application.service.command;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.infrastructure.repository.SprintRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SprintReader {

	private final SprintRepository sprintRepository;

	public Sprint findSprint(
		String sprintKey,
		String workspaceCode
	) {
		return sprintRepository.findBySprintKeyAndWorkspaceCode(sprintKey, workspaceCode)
			.orElseThrow(() -> new ResourceNotFoundException(
				String.format("Sprint was not found with sprint key(%s) and workspace code(%s)",
					sprintKey, workspaceCode))
			);
	}

	public Sprint findSprintWithIssues(
		String sprintKey,
		String workspaceCode
	) {
		return sprintRepository.findBySprintKeyAndWorkspaceCodeWithIssues(sprintKey, workspaceCode)
			.orElseThrow(() -> new ResourceNotFoundException(
				String.format("Sprint was not found with sprint key(%s) and workspace code(%s)",
					sprintKey, workspaceCode))
			);
	}
}
