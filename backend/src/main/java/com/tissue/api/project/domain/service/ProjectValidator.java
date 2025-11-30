package com.tissue.api.project.domain.service;

import org.springframework.stereotype.Component;

import com.tissue.api.project.domain.exception.DuplicateProjectKeyException;
import com.tissue.api.project.domain.port.out.ProjectQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectValidator {

	private final ProjectQueryRepository queryRepository;

	public void ensureUniqueProjectKey(String projectKey, String workspaceKey) {
		if (queryRepository.existsByKeyAndWorkspaceKey(projectKey, workspaceKey)) {
			throw new DuplicateProjectKeyException(projectKey, workspaceKey);
		}
	}

	// TODO: ensureActive (notArchived)
}
