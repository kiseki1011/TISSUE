package com.tissue.issuetype.application.service.validator;

import org.springframework.stereotype.Component;

import com.tissue.common.vo.Label;
import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.IssueField;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnumFieldOptionValidator {

	private final EnumFieldOptionQueryRepository optionRepo;

	public void ensureLabelUnique(IssueField field, Label label) {
		if (optionRepo.existsByIssueFieldAndLabel_Normalized(field, label.getNormalized())) {
			// TODO: DuplicateOptionException
			throw new RuntimeException("Option label already exists in this field.");
		}
	}

	// TODO: ensureDeletable 또는 ensureDeletable 내부에서 호출
	//  EnumFieldOption은 그냥 사용중 여부에 상관 없이 삭제할 수 있도록 할까?
	public void ensureNotInUse(EnumFieldOption opt) {
		if (optionRepo.isInUse(opt)) {
			// TODO: OptionInUseException
			throw new RuntimeException("Cannot delete/archive: option is in use.");
		}
	}
}
