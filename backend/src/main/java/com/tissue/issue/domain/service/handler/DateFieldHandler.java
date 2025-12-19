package com.tissue.issue.domain.service.handler;

import static com.tissue.common.exception.ContextKeys.*;
import static com.tissue.issue.domain.exception.IssueErrorCode.*;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.stereotype.Component;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.FieldType;

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
			throw new BadRequestException(CUSTOM_FIELD_TYPE_MISMATCH)
				.addContext(ISSUE_FIELD_ID, field.getId())
				.addContext(ISSUE_FIELD, field.getDisplayLabel())
				.addContext(EXPECTED_TYPE, field.getFieldType())
				.addContext(INPUT_VALUE, raw);
		}
	}
}
