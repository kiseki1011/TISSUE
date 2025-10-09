package com.tissue.api.issuetype.application.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceConflictException;
import com.tissue.api.issue.domain.model.vo.Label;
import com.tissue.api.issuetype.domain.EnumFieldOption;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.repository.EnumFieldOptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnumFieldOptionValidator {

	private final EnumFieldOptionRepository optionRepo;

	public void ensureLabelUnique(IssueField field, Label label) {
		if (optionRepo.existsByFieldAndLabel_Normalized(field, label.getNormalized())) {
			throw new ResourceConflictException("Option label already exists in this field.");
		}
	}

	public void ensureNotInUse(EnumFieldOption opt) {
		if (optionRepo.isInUse(opt)) {
			throw new InvalidOperationException("Cannot delete/archive: option is in use.");
		}
	}
}
