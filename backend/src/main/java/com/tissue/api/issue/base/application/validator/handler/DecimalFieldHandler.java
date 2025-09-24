package com.tissue.api.issue.base.application.validator.handler;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.InvalidCustomFieldException;
import com.tissue.api.issue.base.domain.enums.FieldType;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.policy.IssueFieldPolicy;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DecimalFieldHandler implements FieldTypeHandler {

	private final IssueFieldPolicy policy; // digits/scale domain rules

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
			throw new InvalidCustomFieldException("must be a decimal number");
		}
	}
}
