package com.tissue.feature.issue.domain.service.handler;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.UNKNOWN_ENUM_OPTION;

import com.tissue.feature.issue.domain.IssueFieldValue;
import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@SuppressWarnings("StringConcatToTextBlock")
public class ChecklistFieldHandler implements FieldTypeHandler {

    @Qualifier("domainConversionService")
    private final ConversionService cs;

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
        Set<Long> validIds = field.getOptions().stream().map(FieldOption::getId).collect(Collectors.toSet());

        for (Long id : inputIds) {
            if (!validIds.contains(id)) {
                throw new BadRequestException(UNKNOWN_ENUM_OPTION).addContext("optionId", id);
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void assign(IssueFieldValue target, @Nullable Object parsed) {
        target.updateChecklistMap((Map<Long, Boolean>) parsed);
    }

    @Override
    public @Nullable Object getValueFrom(IssueFieldValue target) {
        return target.getChecklistMap();
    }
}
