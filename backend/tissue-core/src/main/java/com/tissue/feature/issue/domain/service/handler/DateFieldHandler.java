package com.tissue.feature.issue.domain.service.handler;

import com.tissue.feature.issue.domain.exception.CustomFieldTypeMismatchException;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import java.time.LocalDate;
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
public class DateFieldHandler implements FieldTypeHandler {

    @Qualifier("domainConversionService")
    private final ConversionService cs;

    @Override
    public IssueFieldType type() {
        return IssueFieldType.DATE;
    }

    @Override
    public Object parse(IssueField field, @Nullable Object raw) {
        try {
            return cs.convert(raw, LocalDate.class);
        } catch (ConversionFailedException | ConverterNotFoundException ex) {
            throw new CustomFieldTypeMismatchException(field.getId(), field.getName(), field.getIssueFieldType(), raw);
        }
    }
}
