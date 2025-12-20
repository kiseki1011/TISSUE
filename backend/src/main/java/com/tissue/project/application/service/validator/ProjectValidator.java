package com.tissue.project.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.project.application.port.out.ProjectQueryRepository;
import com.tissue.project.domain.exception.ProjectExceptions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ProjectValidator {

	private final ProjectQueryRepository queryRepository;

	public void ensureUniqueProjectKey(String projectKey, String workspaceKey) {
		if (queryRepository.existsByKeyAndWorkspaceKey(projectKey, workspaceKey)) {
			throw ProjectExceptions.duplicateKey(workspaceKey, projectKey);
		}
	}
}
