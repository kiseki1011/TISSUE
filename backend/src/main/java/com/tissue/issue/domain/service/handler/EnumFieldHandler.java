package com.tissue.issue.domain.service.handler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.FieldType;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnumFieldHandler implements FieldTypeHandler {

	private final EnumFieldOptionQueryRepository optionRepo;

	@Qualifier("domainConversionService")
	private final ConversionService cs;

	@Override
	public FieldType type() {
		return FieldType.ENUM;
	}

	@Override
	public Object parse(@NonNull IssueField field, @NonNull Object raw) {
		try {
			Long optionId = cs.convert(raw, Long.class);
			return optionRepo.findByIdAndIssueField(optionId, field)
				.orElseThrow(() -> new RuntimeException(
					"Unknown enum option(id:%d) for field:%d".formatted(optionId, field.getId())));
		} catch (ConversionFailedException e) {
			throw new RuntimeException("Field(id:%d) must be an enum option id".formatted(field.getId()));
		}
	}
}
