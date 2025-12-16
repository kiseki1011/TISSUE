package com.tissue.issuetype.application.service.finder;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.exception.FieldOptionNotFoundException;

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
