package com.tissue.issuetype.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.common.vo.Label;
import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.exception.IssueTypeExceptions;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnumFieldOptionValidator {

	private final EnumFieldOptionQueryRepository optionRepo;

	public void ensureLabelUnique(IssueField field, Label label) {
		if (optionRepo.existsByIssueFieldAndLabel_Normalized(field, label.getNormalized())) {
			throw IssueTypeExceptions.duplicateOptionName(label, field);
		}
	}

	public void ensureDeletable(EnumFieldOption option) {
		ensureNotInUse(option);
	}

	// TODO: should i just allow the deletion of a option of a ENUM type issue field, whether its in use or not?
	public void ensureNotInUse(EnumFieldOption option) {
		if (optionRepo.isInUse(option)) {
			throw IssueTypeExceptions.optionInUse(option);
		}
	}
}
