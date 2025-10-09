package com.tissue.api.issue.application.validator.handler;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.issuetype.domain.enums.FieldType;
import com.tissue.api.issuetype.domain.IssueField;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TimestampFieldHandler implements FieldTypeHandler {

	@Qualifier("domainConversionService")
	private final ConversionService cs;

	@Override
	public FieldType type() {
		return FieldType.TIMESTAMP;
	}

	@Override
	public Object parse(@NonNull IssueField field, @NonNull Object raw) {
		try {
			return cs.convert(raw, Instant.class);
		} catch (ConversionFailedException | ConverterNotFoundException ex) {
			throw new InvalidCustomFieldException("must be ISO-8601 with offset");
		}
	}
}
