package com.tissue.issue.domain.service.handler;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.stereotype.Component;

import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.IssueFieldType;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class IntegerFieldHandler implements FieldTypeHandler {

	@Qualifier("domainConversionService")
	private final ConversionService cs;

	@Override
	public IssueFieldType type() {
		return IssueFieldType.INTEGER;
	}

	@Override
	public Object parse(@NonNull IssueField field, @NonNull Object raw) {
		try {
			return cs.convert(raw, Integer.class);
		} catch (ConversionFailedException | ConverterNotFoundException ex) {
			throw IssueExceptions.customFieldTypeMismatch(
				field.getId(),
				field.getDisplayName(),
				field.getIssueFieldType(),
				raw
			);
		}
	}
}
