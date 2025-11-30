package com.tissue.api.project.application.service.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.project.domain.Project;
import com.tissue.api.project.domain.exception.ProjectNotFoundException;
import com.tissue.api.project.application.port.out.ProjectQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectFinder {

	private final ProjectQueryRepository queryRepository;

	public Project findBy(String projectKey, String workspaceKey) {
		return queryRepository.findByKeyAndWorkspaceKey(projectKey, workspaceKey)
			.orElseThrow(() -> new ProjectNotFoundException(workspaceKey, projectKey));
	}
}
