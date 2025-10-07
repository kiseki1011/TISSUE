package com.tissue.api.issue.base.application.validator.handler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.infrastructure.repository.EnumFieldOptionRepository;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EnumFieldHandler implements FieldTypeHandler {

	private final EnumFieldOptionRepository optionRepo;

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
			return optionRepo.findByFieldAndId(field, optionId)
				.orElseThrow(() -> new InvalidCustomFieldException(
					"Unknown enum option(id:%d) for field:%d".formatted(optionId, field.getId())));
		} catch (ConversionFailedException e) {
			throw new InvalidCustomFieldException("Field(id:%d) must be an enum option id".formatted(field.getId()));
		}
	}
}
