package com.tissue.api.issue.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.IssueTypeMismatchException;
import com.tissue.api.issue.presentation.dto.request.update.UpdateIssueRequest;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueValidator {

	private final IssueRepository issueRepository;

	public void validateIssueTypeMatch(Issue issue, UpdateIssueRequest request) {
		if (issue.getType() != request.getType()) {
			throw new IssueTypeMismatchException();
		}
	}
}
