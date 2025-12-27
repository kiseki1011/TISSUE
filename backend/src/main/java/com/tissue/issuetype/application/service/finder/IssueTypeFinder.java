package com.tissue.issuetype.application.service.finder;

import org.springframework.stereotype.Component;

import com.tissue.issuetype.application.port.out.IssueTypeQueryRepository;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.exception.IssueTypeExceptions;
import com.tissue.project.domain.Project;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeFinder {

	private final IssueTypeQueryRepository issueTypeQueryRepository;

	public IssueType findBy(Long issueTypeId, Project project) {
		return issueTypeQueryRepository.findByIdAndProject(issueTypeId, project)
			.orElseThrow(() -> IssueTypeExceptions.typeNotFound(issueTypeId, project));
	}
}
