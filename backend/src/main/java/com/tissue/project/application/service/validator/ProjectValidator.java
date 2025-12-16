package com.tissue.project.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.project.domain.exception.DuplicateProjectKeyException;
import com.tissue.project.application.port.out.ProjectQueryRepository;

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
