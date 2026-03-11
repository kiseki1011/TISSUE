package com.tissue.feature.issue.domain.service.handler;

import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@SuppressWarnings("StringConcatToTextBlock")
public class DecimalFieldHandler implements FieldTypeHandler {

    private final IssuePolicy policy;

    @Qualifier("domainConversionService")
    private final ConversionService cs;

    @Override
    public IssueFieldType type() {
        return IssueFieldType.DECIMAL;
    }

    @Override
    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        BigDecimal bd = convert(cs, raw, BigDecimal.class, field);
        if (bd == null) {
            return null;
        }
        policy.ensureDigits(bd);
        return policy.normalizeDecimal(bd);
    }

    @Override
    public @Nullable Object toJsonValue(@Nullable Object domainValue) {
        if (domainValue == null) {
            return null;
        }
        return ((BigDecimal) domainValue).toPlainString();
    }

    @Override
    public @Nullable Object fromJsonValue(@Nullable Object jsonValue) {
        if (jsonValue == null) {
            return null;
        }
        return new BigDecimal((String) jsonValue);
    }
}
