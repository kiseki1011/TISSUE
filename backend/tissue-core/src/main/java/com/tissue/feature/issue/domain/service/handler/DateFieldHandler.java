package com.tissue.feature.issue.domain.service.handler;

import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
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
    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        return convert(cs, raw, LocalDate.class, field);
    }

    @Override
    public @Nullable Object toJsonValue(@Nullable Object domainValue) {
        if (domainValue == null) {
            return null;
        }
        return domainValue.toString();
    }

    @Override
    public @Nullable Object fromJsonValue(@Nullable Object jsonValue) {
        if (jsonValue == null) {
            return null;
        }
        return LocalDate.parse((String) jsonValue);
    }
}
