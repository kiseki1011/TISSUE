package com.tissue.issuetype.application.service.validator;

import com.tissue.global.vo.Name;
import com.tissue.issue.application.port.out.IssueFieldValueQueryRepository;
import com.tissue.issuetype.application.port.out.EnumFieldOptionQueryRepository;
import com.tissue.issuetype.application.port.out.IssueFieldQueryRepository;
import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.issuetype.domain.exception.DuplicateEnumFieldOptionNameException;
import com.tissue.issuetype.domain.exception.DuplicateIssueFieldNameException;
import com.tissue.issuetype.domain.exception.EnumFieldOptionInUseException;
import com.tissue.issuetype.domain.exception.IssueFieldInUseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFieldValidator {

    private final IssueFieldQueryRepository issueFieldRepo;
    private final EnumFieldOptionQueryRepository optionRepo;

    private final IssueFieldValueQueryRepository fieldValueRepo;

    public void ensureUniqueLabel(IssueType issueType, Name name) {
        boolean duplicated = issueFieldRepo.existsByIssueTypeAndName_Normalized(issueType,
            name.getNormalized());
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
