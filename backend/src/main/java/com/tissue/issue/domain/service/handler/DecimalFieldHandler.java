package com.tissue.issue.domain.service.handler;

import static com.tissue.common.exception.ContextKeys.*;
import static com.tissue.issue.domain.exception.IssueErrorCode.*;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.stereotype.Component;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issue.domain.policy.FieldValuePolicy;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.FieldType;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DecimalFieldHandler implements FieldTypeHandler {

	private final FieldValuePolicy policy; // digits/scale domain rules

	@Qualifier("domainConversionService")
	private final ConversionService cs;

	@Override
	public FieldType type() {
		return FieldType.DECIMAL;
	}

	@Override
	public Object parse(@NonNull IssueField field, @NonNull Object raw) {
		try {
			BigDecimal bd = cs.convert(raw, BigDecimal.class);
			policy.ensureDigits(bd, field.getId());
			return policy.normalizeDecimal(bd);
		} catch (ConversionFailedException | ConverterNotFoundException ex) {
			throw new BadRequestException(CUSTOM_FIELD_TYPE_MISMATCH)
				.addContext(ISSUE_FIELD_ID, field.getId())
				.addContext(ISSUE_FIELD, field.getDisplayLabel())
				.addContext(EXPECTED_TYPE, field.getFieldType())
				.addContext(INPUT_VALUE, raw);
		}
	}
}
