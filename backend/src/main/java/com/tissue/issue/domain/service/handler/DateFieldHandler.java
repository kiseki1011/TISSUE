package com.tissue.issue.domain.service.handler;

import com.tissue.issue.domain.exception.IssueExceptions;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import java.time.LocalDate;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.ConverterNotFoundException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DateFieldHandler implements FieldTypeHandler {

    @Qualifier("domainConversionService")
    private final ConversionService cs;

    @Override
    public IssueFieldType type() {
        return IssueFieldType.DATE;
    }

    @Override
    public Object parse(@NonNull IssueField field, @NonNull Object raw) {
        try {
            return cs.convert(raw, LocalDate.class);
        } catch (ConversionFailedException | ConverterNotFoundException ex) {
            throw IssueExceptions.customFieldTypeMismatch(
                    field.getId(), field.getDisplayName(), field.getIssueFieldType(), raw);
        }
    }
}
