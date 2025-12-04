package com.tissue.api.issue.domain.service.handler;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.stereotype.Component;

import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.domain.enums.FieldType;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DateFieldHandler implements FieldTypeHandler {

	@Qualifier("domainConversionService")
	private final ConversionService cs;

	@Override
	public FieldType type() {
		return FieldType.DATE;
	}

	@Override
	public Object parse(@NonNull IssueField field, @NonNull Object raw) {
		try {
			return cs.convert(raw, LocalDate.class);
		} catch (ConversionFailedException | ConverterNotFoundException ex) {
			throw new IllegalArgumentException("must be yyyy-MM-dd");
		}
	}
}
