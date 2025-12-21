package com.tissue.issue.domain.service.handler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.IssueFieldType;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnumFieldHandler implements FieldTypeHandler {

	private final EnumFieldOptionQueryRepository optionRepo;

	@Qualifier("domainConversionService")
	private final ConversionService cs;

	@Override
	public IssueFieldType type() {
		return IssueFieldType.ENUM;
	}

	@Override
	public Object parse(@NonNull IssueField field, @NonNull Object raw) {
		try {
			Long optionId = cs.convert(raw, Long.class);
			return optionRepo.findByIdAndIssueField(optionId, field)
				.orElseThrow(() -> IssueExceptions.unknownEnumOption(field.getId(), optionId));
		} catch (ConversionFailedException e) {
			throw IssueExceptions.customFieldTypeMismatch(
				field.getId(),
				field.getDisplayLabel(),
				field.getIssueFieldType(),
				raw
			);
		}
	}
}
