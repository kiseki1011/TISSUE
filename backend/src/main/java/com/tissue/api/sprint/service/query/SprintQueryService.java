package com.tissue.api.sprint.service.query;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.sprint.domain.Sprint;
import com.tissue.api.sprint.domain.repository.SprintRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintQueryService {

	private final SprintRepository sprintRepository;

	@Transactional(readOnly = true)
	public Sprint findSprint(String sprintKey, String workspaceCode) {

		return sprintRepository.findBySprintKeyAndWorkspaceCode(sprintKey, workspaceCode)
			.orElseThrow(() -> new ResourceNotFoundException(
				String.format("Sprint was not found with sprint key(%s) and workspace code(%s)",
					sprintKey, workspaceCode)));
	}
}
