package com.tissue.feature.issue.domain.service.handler;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.INVALID_PERCENTAGE_EXCEPTION;

import com.tissue.feature.issue.domain.IssueFieldValue;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.exception.base.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@SuppressWarnings("StringConcatToTextBlock")
public class PercentageFieldHandler implements FieldTypeHandler {

    @Qualifier("domainConversionService")
    private final ConversionService cs;

    @Override
    public IssueFieldType type() {
        return IssueFieldType.PERCENTAGE;
    }

    @Override
    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        Integer val = convert(cs, raw, Integer.class, field);
        if (val == null) {
            return null;
        }
        if (val < 0 || val > 100) {
            throw new BadRequestException(INVALID_PERCENTAGE_EXCEPTION);
        }
        return val;
    }

    @Override
    public void assign(IssueFieldValue target, @Nullable Object parsed) {
        target.updateInteger((Integer) parsed);
    }

    @Override
    public @Nullable Object getValueFrom(IssueFieldValue target) {
        return target.getIntegerValue();
    }
}
