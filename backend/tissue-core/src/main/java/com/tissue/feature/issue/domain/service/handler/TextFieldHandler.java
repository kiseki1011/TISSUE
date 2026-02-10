package com.tissue.feature.issue.domain.service.handler;

import com.tissue.feature.issue.domain.exception.CustomFieldTypeMismatchException;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
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
public class TextFieldHandler implements FieldTypeHandler {

    @Qualifier("domainConversionService")
    private final ConversionService cs;

    @Override
    public IssueFieldType type() {
        return IssueFieldType.TEXT;
    }

    @Override
    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        try {
            return cs.convert(raw, String.class);
            // TODO: Is it the client's fault for ConverterNotFoundException?
        } catch (ConversionFailedException | ConverterNotFoundException ex) {
            throw new CustomFieldTypeMismatchException(field.getId(), field.getName(), field.getIssueFieldType(), raw);
        }
    }
}
