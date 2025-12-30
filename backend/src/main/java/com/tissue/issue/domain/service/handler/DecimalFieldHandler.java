package com.tissue.issue.domain.service.handler;

import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.issue.domain.policy.FieldValuePolicy;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@SuppressWarnings("StringConcatToTextBlock")
public class DecimalFieldHandler implements FieldTypeHandler {

    private final FieldValuePolicy policy; // digits/scale rules

    @Qualifier("domainConversionService")
    private final ConversionService cs;

    @Override
    public IssueFieldType type() {
        return IssueFieldType.DECIMAL;
    }

    @Override
    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        try {
            BigDecimal bd = cs.convert(raw, BigDecimal.class);
            policy.ensureDigits(bd, field.getId());
            return policy.normalizeDecimal(bd);
        } catch (ConversionFailedException | ConverterNotFoundException ex) {
            throw IssueExceptions.customFieldTypeMismatch(
                    field.getId(), field.getDisplayName(), field.getIssueFieldType(), raw);
        }
    }
}
