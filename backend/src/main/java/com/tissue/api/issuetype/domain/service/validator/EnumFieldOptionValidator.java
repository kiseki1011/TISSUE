package com.tissue.api.issuetype.domain.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.BadRequestException;
import com.tissue.api.common.exception.type.ResourceConflictException;
import com.tissue.api.common.vo.Label;
import com.tissue.api.issuetype.domain.EnumFieldOption;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.repository.EnumFieldOptionQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnumFieldOptionValidator {

	private final EnumFieldOptionQueryRepository optionRepo;

	public void ensureLabelUnique(IssueField field, Label label) {
		if (optionRepo.existsByIssueFieldAndLabel_Normalized(field, label.getNormalized())) {
			throw new ResourceConflictException("Option label already exists in this field.");
		}
	}

	public void ensureNotInUse(EnumFieldOption opt) {
		if (optionRepo.isInUse(opt)) {
			throw new BadRequestException("Cannot delete/archive: option is in use.");
		}
	}
}
