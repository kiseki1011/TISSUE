package com.tissue.issue.domain.service.handler;

import com.tissue.issue.domain.exception.CustomFieldTypeMismatchException;
import com.tissue.issue.domain.exception.UnknownEnumOptionException;
import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@SuppressWarnings("StringConcatToTextBlock")
public class EnumFieldHandler implements FieldTypeHandler {

    private final EnumFieldOptionQueryRepository optionRepo;

    @Qualifier("domainConversionService")
    private final ConversionService cs;

    @Override
    public IssueFieldType type() {
        return IssueFieldType.ENUM;
    }

    @Override
    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        try {
            Long optionId = cs.convert(raw, Long.class);
            return optionRepo
                    .findByIdAndIssueField(optionId, field)
                    .orElseThrow(() -> new UnknownEnumOptionException(field.getId(), optionId));
        } catch (ConversionFailedException e) {
            throw new CustomFieldTypeMismatchException(field.getId(), field.getName(), field.getIssueFieldType(), raw);
        }
    }
}
