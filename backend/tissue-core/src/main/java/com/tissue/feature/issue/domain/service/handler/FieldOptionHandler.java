package com.tissue.feature.issue.domain.service.handler;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.UNKNOWN_ENUM_OPTION;

import com.tissue.feature.issuetype.application.port.repository.FieldOptionRepository;
import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.exception.base.BadRequestException;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
public class FieldOptionHandler implements FieldTypeHandler {

    private final FieldOptionRepository optionRepo;
    private final ConversionService cs;

    public FieldOptionHandler(
            FieldOptionRepository optionRepo, @Qualifier("domainConversionService") ConversionService cs) {
        this.optionRepo = optionRepo;
        this.cs = cs;
    }

    @Override
    public IssueFieldType type() {
        return IssueFieldType.SELECT_OPTION;
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
    public @Nullable Object toJsonValue(@Nullable Object domainValue) {
        if (domainValue == null) {
            return null;
        }
        return ((FieldOption) domainValue).getId();
    }

    @Override
    public @Nullable Object fromJsonValue(@Nullable Object jsonValue) {
        if (jsonValue == null) {
            return null;
        }
        return ((Number) jsonValue).longValue();
    }
}
