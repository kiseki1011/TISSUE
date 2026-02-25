package com.tissue.feature.issue.application.service.validator;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.CUSTOM_FIELD_REQUIRED;
import static com.tissue.feature.issue.domain.exception.IssueErrorCode.UNKNOWN_CUSTOM_FIELD_ID;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueFieldValue;
import com.tissue.feature.issue.domain.service.handler.IssueFieldTypeHandlerRegistry;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFieldSchemaValidator {

    private final IssueFieldRepository issueFieldRepo;
    private final IssueFieldTypeHandlerRegistry fieldTypeHandler;

    public void validateAndAssign(Map<Long, Object> rawInputById, Issue issue) {
        List<IssueField> fields = loadFields(issue);
        Map<Long, IssueFieldValue> valueMap = buildValueMap(issue);

        for (IssueField field : fields) {
            processField(issue, field, rawInputById.get(field.getId()), valueMap.get(field.getId()));
        }
    }

    public void validateAndApplyPatch(Map<Long, Object> rawInputById, Issue issue) {
        Map<Long, IssueField> schemaMap = loadFieldMap(issue);
        Map<Long, IssueFieldValue> valueMap = buildValueMap(issue);

        for (Map.Entry<Long, Object> e : rawInputById.entrySet()) {
            IssueField field = getKnownField(schemaMap, e.getKey());
            processField(issue, field, e.getValue(), valueMap.get(field.getId()));
        }
    }

    private Map<Long, IssueFieldValue> buildValueMap(Issue issue) {
        return issue.getFieldValues().stream()
                .collect(Collectors.toMap(fv -> fv.getField().getId(), fv -> fv));
    }

    private void processField(
            Issue issue, IssueField field, @Nullable Object raw, @Nullable IssueFieldValue existingValue) {

        ensureValueExistsIfRequired(field, raw);

        if (isEmptyValue(field, raw)) {
            Optional.ofNullable(existingValue).ifPresent(IssueFieldValue::clearValue);
            return;
        }

        IssueFieldValue fieldValueEntity = (existingValue != null) ? existingValue : issue.addFieldValue(field);

        Object parsed = fieldTypeHandler.parse(field, raw);
        fieldTypeHandler.assign(fieldValueEntity, parsed);
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
}
