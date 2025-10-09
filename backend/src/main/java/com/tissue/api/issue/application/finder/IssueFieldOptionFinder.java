package com.tissue.api.issue.application.finder;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tissue.api.issuetype.domain.EnumFieldOption;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.repository.EnumFieldOptionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IssueFieldOptionFinder {

	private final EnumFieldOptionRepository optionRepo;

	public EnumFieldOption findOption(IssueField field, Long optionId) {
		return optionRepo.findByFieldAndId(field, optionId)
			.orElseThrow(() -> new EntityNotFoundException("Option not found."));
	}

	public List<EnumFieldOption> findActiveOptions(IssueField field) {
		return optionRepo.findByFieldOrderByPositionAsc(field);
	}
}
