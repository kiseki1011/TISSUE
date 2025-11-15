package com.tissue.api.issuetype.domain.service.validator;

import org.springframework.stereotype.Component;

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
			// TODO: DuplicateOptionException
			throw new RuntimeException("Option label already exists in this field.");
		}
	}

	// TODO: ensureDeletable 또는 ensureDeletable 내부에서 호출
	//  EnumFieldOption은 그냥 사용중 여부에 상관 없이 삭제할 수 있도록 할까?
	public void ensureNotInUse(EnumFieldOption opt) {
		if (optionRepo.isInUse(opt)) {
			// TODO: OptionNotDeletableException vs OptionCurrentlyUsedException vs OptionInUseNotDeletableException
			//  이름을 어떻게 정하는게 좋을지 모르겠음. 상황을 설명? or 원인을 설명?
			throw new RuntimeException("Cannot delete/archive: option is in use.");
		}
	}
}
