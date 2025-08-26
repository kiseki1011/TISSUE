package com.tissue.api.issue.base.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.base.application.dto.CreateIssueFieldCommand;
import com.tissue.api.issue.base.application.dto.UpdateIssueFieldCommand;
import com.tissue.api.issue.base.application.finder.IssueFieldFinder;
import com.tissue.api.issue.base.application.finder.IssueTypeFinder;
import com.tissue.api.issue.base.application.validator.IssueFieldValidator;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.issue.base.infrastructure.repository.IssueFieldRepository;
import com.tissue.api.issue.base.presentation.dto.response.IssueFieldResponse;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueFieldService {

	private final IssueTypeFinder issueTypeFinder;
	private final IssueFieldFinder issueFieldFinder;
	private final IssueFieldRepository issueFieldRepo;
	private final IssueFieldValidator issueFieldValidator;

	@Transactional
	public IssueFieldResponse createIssueField(CreateIssueFieldCommand cmd) {
		IssueType issueType = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeKey());

		issueFieldValidator.ensureUniqueLabel(issueType, cmd.label());

		IssueField issueField = issueFieldRepo.save(IssueField.builder()
			.label(cmd.label())
			.description(cmd.description())
			.fieldType(cmd.fieldType())
			.required(Boolean.TRUE.equals(cmd.required()))
			.issueType(issueType)
			.allowedOptions(cmd.allowedOptions())
			.build());

		return IssueFieldResponse.from(issueField);
	}

	@Transactional
	public IssueFieldResponse updateIssueField(UpdateIssueFieldCommand cmd) {
		IssueType type = issueTypeFinder.findIssueType(cmd.workspaceKey(), cmd.issueTypeKey());
		IssueField field = issueFieldFinder.findIssueField(type, cmd.issueFieldKey());

		issueFieldValidator.ensureUniqueLabel(type, cmd.label(), field.getId());

		field.updateMetaData(cmd.label(), cmd.description(), cmd.required());

		return IssueFieldResponse.from(field);
	}

	// TODO: Add deleteIssueField
}
