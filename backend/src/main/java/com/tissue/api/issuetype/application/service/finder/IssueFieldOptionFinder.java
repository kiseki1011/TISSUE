package com.tissue.api.issuetype.application.service.finder;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.issuetype.domain.EnumFieldOption;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.exception.FieldOptionNotFoundException;
import com.tissue.api.issuetype.repository.EnumFieldOptionQueryRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldOptionFinder {

	private final EnumFieldOptionQueryRepository optionQueryRepo;

	public EnumFieldOption findByIdAndIssueField(Long optionId, IssueField field) {
		return optionQueryRepo.findByIdAndIssueField(optionId, field)
			.orElseThrow(() -> new FieldOptionNotFoundException(optionId, field.getId()));
	}

	public List<EnumFieldOption> findActiveOptions(IssueField field) {
		return optionQueryRepo.findByIssueFieldOrderByPositionAsc(field);
	}
}
