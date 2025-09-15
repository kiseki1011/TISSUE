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

	public IssueField findIssueField(Long id) {
		return issueFieldRepo.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Issue field not found."));
	}

	public IssueField findIssueField(IssueType issueType, Long id) {
		return issueFieldRepo.findByIssueTypeAndId(issueType, id)
			.orElseThrow(() -> new ResourceNotFoundException("Issue field not found."));
	}
}
