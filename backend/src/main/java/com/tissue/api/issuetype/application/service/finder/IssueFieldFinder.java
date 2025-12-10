package com.tissue.api.issuetype.application.service.finder;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.issuetype.application.port.out.IssueFieldQueryRepository;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.issuetype.domain.exception.IssueFieldNotFoundException;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldFinder {

	private final IssueFieldQueryRepository issueFieldRepo;

	public IssueField findBy(Long issueFieldId, IssueType issueType) {
		return issueFieldRepo.findByIdAndIssueType(issueFieldId, issueType)
			.orElseThrow(() -> new IssueFieldNotFoundException(issueFieldId, issueType.getId()));
	}

	public List<IssueField> findByIssueType(IssueType issueType) {
		return issueFieldRepo.findByIssueType(issueType);
	}
}
