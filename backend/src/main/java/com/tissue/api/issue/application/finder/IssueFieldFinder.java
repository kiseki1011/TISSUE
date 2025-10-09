package com.tissue.api.issue.application.finder;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.repository.IssueFieldRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldFinder {

	private final IssueFieldRepository issueFieldRepo;

	public IssueField findById(Long id) {
		return issueFieldRepo.findById(id)
			.orElseThrow(() -> new ResourceNotFoundException("Issue field not found."));
	}

	public IssueField findByTypeAndId(IssueType issueType, Long id) {
		return issueFieldRepo.findByIssueTypeAndId(issueType, id)
			.orElseThrow(() -> new ResourceNotFoundException("Issue field not found."));
	}

	public List<IssueField> findByIssueType(IssueType issueType) {
		return issueFieldRepo.findByIssueType(issueType);
	}
}
