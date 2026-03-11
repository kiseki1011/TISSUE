package com.tissue.feature.issue.domain.service.handler;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.UNKNOWN_ENUM_OPTION;

import com.tissue.feature.issue.domain.exception.UnknownEnumOptionException;
import com.tissue.feature.issuetype.application.port.repository.FieldOptionRepository;
import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

@Component
public class ChecklistFieldHandler implements FieldTypeHandler {

    private final FieldOptionRepository optionRepo;
    private final ConversionService cs;

    public ChecklistFieldHandler(
            FieldOptionRepository optionRepo, @Qualifier("domainConversionService") ConversionService cs) {
        this.optionRepo = optionRepo;
        this.cs = cs;
    }

    @Override
    public IssueFieldType type() {
        return IssueFieldType.CHECKLIST;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Object parse(IssueField field, @Nullable Object raw) {
        if (raw == null) {
            return null;
        }

        Map<Long, Boolean> inputMap = (Map<Long, Boolean>) cs.convert(
                raw,
                TypeDescriptor.forObject(raw),
                TypeDescriptor.map(
                        Map.class, TypeDescriptor.valueOf(Long.class), TypeDescriptor.valueOf(Boolean.class)));

        if (inputMap == null) {
            return null;
        }

        validateOptionIds(field, inputMap.keySet());

        return inputMap;
    }

    private void validateOptionIds(IssueField field, Set<Long> inputIds) {
        if (inputIds.isEmpty()) {
            return;
        }

        List<FieldOption> options = optionRepo.findAllById(inputIds);

        if (options.size() != inputIds.size()) {
            throw new BadRequestException(UNKNOWN_ENUM_OPTION);
        }

        for (FieldOption option : options) {
            if (!Objects.equals(option.getIssueField(), field)) {
                throw new UnknownEnumOptionException(field.getId(), option.getId());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Object toJsonValue(@Nullable Object domainValue) {
        if (domainValue == null) {
            return null;
        }
        Map<Long, Boolean> map = (Map<Long, Boolean>) domainValue;
        Map<String, Boolean> jsonMap = new HashMap<>();
        for (Map.Entry<Long, Boolean> entry : map.entrySet()) {
            jsonMap.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return jsonMap;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable Object fromJsonValue(@Nullable Object jsonValue) {
        if (jsonValue == null) {
            return null;
        }
        Map<String, Boolean> jsonMap = (Map<String, Boolean>) jsonValue;
        Map<Long, Boolean> domainMap = new HashMap<>();
        for (Map.Entry<String, Boolean> entry : jsonMap.entrySet()) {
            domainMap.put(Long.valueOf(entry.getKey()), entry.getValue());
        }
        return domainMap;
    }
}
