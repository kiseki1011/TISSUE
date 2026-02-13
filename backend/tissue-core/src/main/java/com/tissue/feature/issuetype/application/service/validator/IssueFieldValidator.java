package com.tissue.feature.issuetype.application.service.validator;

import com.tissue.feature.issue.application.port.repository.IssueFieldValueQueryRepository;
import com.tissue.feature.issuetype.application.port.repository.EnumFieldOptionRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.domain.EnumFieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.exception.DuplicateEnumFieldOptionNameException;
import com.tissue.feature.issuetype.domain.exception.DuplicateIssueFieldNameException;
import com.tissue.feature.issuetype.domain.exception.EnumFieldOptionInUseException;
import com.tissue.feature.issuetype.domain.exception.IssueFieldInUseException;
import com.tissue.shared.vo.Name;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFieldValidator {

    private final IssueFieldRepository issueFieldRepo;
    private final EnumFieldOptionRepository optionRepo;

    private final IssueFieldValueQueryRepository fieldValueRepo;

    public void ensureUniqueLabel(IssueType issueType, Name name) {
        boolean duplicated = issueFieldRepo.existsByIssueTypeAndName_Normalized(issueType, name.getNormalized());
        if (duplicated) {
            throw new DuplicateIssueFieldNameException(name, issueType);
        }
    }

    public void ensureDeletable(IssueField issueField) {
        ensureFieldNotInUse(issueField);
    }

    public void ensureUniqueOptionLabel(IssueField field, Name name) {
        if (optionRepo.existsByIssueFieldAndName_Normalized(field, name.getNormalized())) {
            throw new DuplicateEnumFieldOptionNameException(name, field);
        }
    }

    public void ensureOptionDeletable(EnumFieldOption option) {
        ensureOptionNotInUse(option);
    }

    private void ensureFieldNotInUse(IssueField issueField) {
        if (fieldValueRepo.existsByField(issueField)) {
            throw new IssueFieldInUseException(issueField);
        }
    }

    private void ensureOptionNotInUse(EnumFieldOption option) {
        if (optionRepo.isInUse(option)) {
            throw new EnumFieldOptionInUseException(option);
        }
    }
}
