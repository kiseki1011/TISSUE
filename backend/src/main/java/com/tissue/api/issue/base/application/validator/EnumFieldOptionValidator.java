package com.tissue.api.issue.base.application.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.common.exception.type.ResourceConflictException;
import com.tissue.api.issue.base.domain.model.EnumFieldOption;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.infrastructure.repository.EnumFieldOptionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnumFieldOptionValidator {

	private final EnumFieldOptionRepository optionRepo;

	public void ensureLabelUnique(IssueField field, String label) {
		if (optionRepo.existsByFieldAndLabel(field, label)) {
			throw new ResourceConflictException("Option label already exists in this field.");
		}
	}

	public void ensureNotInUse(EnumFieldOption opt) {
		if (optionRepo.isInUse(opt)) {
			throw new InvalidOperationException("Cannot delete/archive: option is in use.");
		}
	}
}
