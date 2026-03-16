package com.tissue.feature.issue.application.service.validator;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.CUSTOM_FIELD_REQUIRED;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.UNKNOWN_CUSTOM_FIELD_ID;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.service.handler.IssueFieldTypeHandlerRegistry;
import com.tissue.feature.issuetype.application.port.repository.FieldOptionRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

@Component
public class CustomFieldSchemaProcessor {

    private final IssueFieldRepository issueFieldRepo;
    private final FieldOptionRepository enumOptionRepo;
    private final IssueFieldTypeHandlerRegistry fieldTypeHandler;
    private final ConversionService conversionService;

    public CustomFieldSchemaProcessor(
            IssueFieldRepository issueFieldRepo,
            FieldOptionRepository enumOptionRepo,
            IssueFieldTypeHandlerRegistry fieldTypeHandler,
            @Qualifier("domainConversionService") ConversionService conversionService) {
        this.issueFieldRepo = issueFieldRepo;
        this.enumOptionRepo = enumOptionRepo;
        this.fieldTypeHandler = fieldTypeHandler;
        this.conversionService = conversionService;
    }

    public void validateAndAssign(Map<Long, Object> rawInputById, Issue issue) {
        List<IssueField> fields = loadFields(issue);
        Map<Long, IssueField> schemaMap = loadFieldMap(issue);
        bulkLoadFieldOptions(rawInputById, schemaMap);

        for (IssueField field : fields) {
            processField(issue, field, rawInputById.get(field.getId()));
        }
    }

    public void validateAndApplyPatch(Map<Long, Object> rawInputById, Issue issue) {
        Map<Long, IssueField> schemaMap = loadFieldMap(issue);
        bulkLoadFieldOptions(rawInputById, schemaMap);

        for (Map.Entry<Long, Object> e : rawInputById.entrySet()) {
            IssueField field = getKnownField(schemaMap, e.getKey());
            processField(issue, field, e.getValue());
        }
    }

    private void processField(Issue issue, IssueField field, @Nullable Object raw) {
        ensureValueExistsIfRequired(field, raw);

        String fieldIdStr = String.valueOf(field.getId());

        if (isEmptyValue(field, raw)) {
            issue.clearCustomField(fieldIdStr);
            return;
        }

        Object parsed = fieldTypeHandler.parse(field, raw);
        Object jsonValue = fieldTypeHandler.toJsonValue(field, parsed);
        if (jsonValue == null) {
            issue.clearCustomField(fieldIdStr);
            return;
        }
        issue.setCustomFieldValue(fieldIdStr, jsonValue);
    }

    private Map<Long, IssueField> loadFieldMap(Issue issue) {
        return loadFields(issue).stream().collect(Collectors.toMap(IssueField::getId, field -> field));
    }

    private List<IssueField> loadFields(Issue issue) {
        return issueFieldRepo.findByIssueType(issue.getIssueType());
    }

    private void ensureValueExistsIfRequired(IssueField field, @Nullable Object raw) {
        if (field.isRequired() && isEmptyValue(field, raw)) {
            throw new BadRequestException(CUSTOM_FIELD_REQUIRED);
        }
    }

    private boolean isEmptyValue(IssueField field, @Nullable Object raw) {
        return fieldTypeHandler.isBlank(field, raw);
    }

    private IssueField getKnownField(Map<Long, IssueField> map, Long id) {
        IssueField field = map.get(id);
        if (field == null) {
            throw new BadRequestException(UNKNOWN_CUSTOM_FIELD_ID);
        }
        return field;
    }

    private void bulkLoadFieldOptions(Map<Long, Object> rawInputById, Map<Long, IssueField> schemaMap) {
        Set<Long> optionIds = rawInputById.entrySet().stream()
                .filter(entry -> {
                    IssueField field = schemaMap.get(entry.getKey());
                    return field != null && field.getIssueFieldType().canHaveOptions();
                })
                .flatMap(entry -> {
                    Object val = entry.getValue();
                    if (val instanceof Map<?, ?> map) {
                        return map.keySet().stream();
                    }
                    return Stream.of(val);
                })
                .filter(Objects::nonNull)
                .map(val -> {
                    try {
                        return conversionService.convert(val, Long.class);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (!optionIds.isEmpty()) {
            enumOptionRepo.findAllById(optionIds);
        }
    }
}
