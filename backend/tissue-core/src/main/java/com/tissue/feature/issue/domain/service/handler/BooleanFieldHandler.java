package com.tissue.feature.issue.domain.service.handler;

import com.tissue.feature.issue.domain.IssueFieldValue;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@SuppressWarnings("StringConcatToTextBlock")
public class BooleanFieldHandler implements FieldTypeHandler {

    @Qualifier("domainConversionService")
    private final ConversionService cs;

    @Override
    public IssueFieldType type() {
        return IssueFieldType.BOOLEAN;
    }

    @Override
    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        return convert(cs, raw, Boolean.class, field);
    }

    @Override
    public void assign(IssueFieldValue target, @Nullable Object parsed) {
        target.updateBoolean((Boolean) parsed);
    }

    @Override
    public @Nullable Object getValueFrom(IssueFieldValue target) {
        return target.getBooleanValue();
    }
}
