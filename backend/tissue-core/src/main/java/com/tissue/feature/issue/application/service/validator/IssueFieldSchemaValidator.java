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

        for (IssueField field : fields) {
            Object raw = rawInputById.get(field.getId());
            ensureValueExistsIfRequired(field, raw);

            if (isEmptyValue(field, raw)) {
                continue;
            }

            IssueFieldValue val = issue.addOrUpdateFieldValue(field);
            parseAndAssignValue(val, field, raw);
        }
    }

    public void validateAndApplyPatch(Map<Long, Object> rawInputById, Issue issue) {
        Map<Long, IssueField> defMap = loadFieldMap(issue);

        for (Map.Entry<Long, Object> e : rawInputById.entrySet()) {
            applyOnePatchEntry(issue, defMap, e.getKey(), e.getValue());
        }
    }

    private void applyOnePatchEntry(Issue issue, Map<Long, IssueField> fieldMap, Long fieldId, Object raw) {
        IssueField field = requireKnownField(fieldMap, fieldId);
        ensureValueExistsIfRequired(field, raw);

        IssueFieldValue fieldValue = issue.addOrUpdateFieldValue(field);

        if (isEmptyValue(field, raw)) {
            fieldValue.clearValue();
            return;
        }

        Object parsed = fieldTypeHandler.parse(field, raw);
        fieldTypeHandler.assign(fieldValue, parsed);
    }

    private List<IssueField> loadFields(Issue issue) {
        return issueFieldRepo.findByIssueType(issue.getIssueType());
    }

    private Map<Long, IssueField> loadFieldMap(Issue issue) {
        return loadFields(issue).stream().collect(Collectors.toMap(IssueField::getId, it -> it));
    }

    private boolean isEmptyValue(IssueField field, @Nullable Object raw) {
        return fieldTypeHandler.isBlank(field, raw);
    }

    private void parseAndAssignValue(IssueFieldValue val, IssueField field, @Nullable Object raw) {
        Object parsed = fieldTypeHandler.parse(field, raw);
        fieldTypeHandler.assign(val, parsed);
    }

    private void ensureValueExistsIfRequired(IssueField field, @Nullable Object raw) {
        boolean fieldNotRequired = !field.isRequired();
        if (fieldNotRequired) {
            return;
        }
        if (isEmptyValue(field, raw)) {
            throw new BadRequestException(CUSTOM_FIELD_REQUIRED);
        }
    }

    private IssueField requireKnownField(Map<Long, IssueField> map, Long id) {
        IssueField field = map.get(id);
        if (field == null) {
            throw new BadRequestException(UNKNOWN_CUSTOM_FIELD_ID);
        }
        return field;
    }
}
