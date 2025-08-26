package com.tissue.api.issue.base.application.finder;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldFinder {

	private final IssueFieldRepository issueFieldRepo;

	public IssueField findIssueField(String key) {
		return issueFieldRepo.findByKey(key)
			.orElseThrow(() -> new ResourceNotFoundException("Issue field not found."));
	}

	public IssueField findIssueField(IssueType issueType, String key) {
		return issueFieldRepo.findByIssueTypeAndKey(issueType, key)
			.orElseThrow(() -> new ResourceNotFoundException("Issue field not found."));
	}
}
