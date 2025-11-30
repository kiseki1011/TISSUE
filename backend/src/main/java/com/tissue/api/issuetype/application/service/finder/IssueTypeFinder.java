package com.tissue.api.issuetype.application.service.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.exception.IssueTypeNotFoundException;
import com.tissue.api.issuetype.repository.IssueTypeQueryRepository;
import com.tissue.api.project.domain.Project;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeFinder {

	private final IssueTypeQueryRepository issueTypeQueryRepository;

	public IssueType findBy(Long issueTypeId, String projectKey, String workspaceKey) {
		return issueTypeQueryRepository.findByIdAndProjectKeyAndWorkspaceKey(issueTypeId, projectKey, workspaceKey)
			.orElseThrow(() -> new IssueTypeNotFoundException(issueTypeId, projectKey, workspaceKey));
	}

	public IssueType findBy(Long issueTypeId, Project project) {
		return issueTypeQueryRepository.findByIdAndProject(issueTypeId, project)
			.orElseThrow(
				() -> new IssueTypeNotFoundException(issueTypeId, project.getKey(), project.getWorkspaceKey()));
	}
}
