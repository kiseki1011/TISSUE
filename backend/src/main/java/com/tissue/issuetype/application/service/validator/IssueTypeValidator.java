package com.tissue.issuetype.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.common.vo.Name;
import com.tissue.issue.application.port.out.IssueFieldValueQueryRepository;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.application.port.out.IssueFieldQueryRepository;
import com.tissue.issuetype.application.port.out.IssueTypeQueryRepository;
import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.exception.IssueTypeExceptions;
import com.tissue.project.domain.Project;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueTypeValidator {

	private final IssueTypeQueryRepository issueTypeQueryRepo;
	private final IssueFieldQueryRepository issueFieldRepo;
	private final EnumFieldOptionQueryRepository optionRepo;

	private final IssueQueryRepository issueQueryRepo;
	private final IssueFieldValueQueryRepository fieldValueRepo;

	public void ensureUniqueLabel(Project project, Name name) {
		boolean duplicated = issueTypeQueryRepo.existsByName_NormalizedAndProject(name.getNormalized(), project);
		if (duplicated) {
			throw IssueTypeExceptions.duplicateTypeName(name, project);
		}
	}

	public void ensureDeletable(IssueType type) {
		ensureTypeNotInUse(type);
	}

	public void ensureUniqueFieldLabel(IssueType issueType, Name name) {
		boolean duplicated = issueFieldRepo.existsByIssueTypeAndName_Normalized(issueType, name.getNormalized());
		if (duplicated) {
			throw IssueTypeExceptions.duplicateFieldName(name, issueType);
		}
	}

	public void ensureFieldDeletable(IssueField issueField) {
		ensureFieldNotInUse(issueField);
	}

	public void ensureUniqueOptionLabel(IssueField field, Name name) {
		if (optionRepo.existsByIssueFieldAndName_Normalized(field, name.getNormalized())) {
			throw IssueTypeExceptions.duplicateOptionName(name, field);
		}
	}

	public void ensureOptionDeletable(EnumFieldOption option) {
		ensureOptionNotInUse(option);
	}

	private void ensureTypeNotInUse(IssueType issueType) {
		if (issueQueryRepo.existsByIssueType(issueType)) {
			throw IssueTypeExceptions.typeInUse(issueType);
		}
	}

	private void ensureFieldNotInUse(IssueField issueField) {
		if (fieldValueRepo.existsByField(issueField)) {
			throw IssueTypeExceptions.fieldInUse(issueField);
		}
	}

	private void ensureOptionNotInUse(EnumFieldOption option) {
		if (optionRepo.isInUse(option)) {
			throw IssueTypeExceptions.optionInUse(option);
		}
	}
}
