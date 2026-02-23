package com.tissue.feature.issue.domain.service.handler;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.UNKNOWN_ENUM_OPTION;

import com.tissue.feature.issue.domain.IssueFieldValue;
import com.tissue.feature.issuetype.application.port.repository.EnumFieldOptionRepository;
import com.tissue.feature.issuetype.domain.EnumFieldOption;
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
public class EnumFieldHandler implements FieldTypeHandler {

    private final EnumFieldOptionRepository optionRepo;

    @Qualifier("domainConversionService")
    private final ConversionService cs;

    @Override
    public IssueFieldType type() {
        return IssueFieldType.ENUM;
    }

    @Override
    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        Long optionId = convert(cs, raw, Long.class, field);
        if (optionId == null) {
            return null;
        }

        return optionRepo
                .findByIdAndIssueField(optionId, field)
                .orElseThrow(() -> new BadRequestException(UNKNOWN_ENUM_OPTION));
    }

    @Override
    public void assign(IssueFieldValue target, @Nullable Object parsed) {
        target.updateEnum((EnumFieldOption) parsed);
    }

    @Override
    public @Nullable Object getValueFrom(IssueFieldValue target) {
        return target.getEnumOption();
    }
}
