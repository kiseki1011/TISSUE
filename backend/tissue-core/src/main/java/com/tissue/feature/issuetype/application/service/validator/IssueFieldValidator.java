package com.tissue.feature.issuetype.application.service.validator;

import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.DUPLICATE_ISSUE_FIELD_NAME;
import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.ISSUE_FIELD_IN_USE;
import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.ISSUE_FIELD_OPTION_IN_USE;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_ID;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_NAME;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_FIELD_OPTION_NAME;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_TYPE_ID;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_TYPE_NAME;

import com.tissue.feature.issue.application.port.repository.IssueFieldValueQueryRepository;
import com.tissue.feature.issuetype.application.port.repository.FieldOptionRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueFieldRepository;
import com.tissue.feature.issuetype.domain.FieldOption;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IssueFieldValidator {

    private final IssueFieldRepository issueFieldRepo;
    private final FieldOptionRepository optionRepo;

    private final IssueFieldValueQueryRepository fieldValueRepo;

    public void ensureUniqueLabel(IssueType issueType, Name name) {
        boolean duplicated = issueFieldRepo.existsByIssueTypeAndName_Normalized(issueType, name.getNormalized());
        if (duplicated) {
            throw new ResourceConflictException(DUPLICATE_ISSUE_FIELD_NAME)
                    .addContext(ISSUE_TYPE_ID, issueType.getId())
                    .addContext(ISSUE_TYPE_NAME, issueType.getName())
                    .addContext(ISSUE_FIELD_NAME, name);
        }
    }

    public void ensureDeletable(IssueField issueField) {
        ensureFieldNotInUse(issueField);
    }

    public void ensureUniqueOptionLabel(IssueField field, Name name) {
        if (optionRepo.existsByIssueFieldAndName_Normalized(field, name.getNormalized())) {
            throw new ResourceConflictException(IssueTypeErrorCode.DUPLICATE_FIELD_OPTION_NAME)
                    .addContext(ISSUE_FIELD_ID, field.getId())
                    .addContext(ISSUE_FIELD_NAME, field.getName())
                    .addContext(ISSUE_FIELD_OPTION_NAME, name);
        }
    }

    public void ensureOptionDeletable(FieldOption option) {
        ensureOptionNotInUse(option);
    }

    private void ensureFieldNotInUse(IssueField issueField) {
        if (fieldValueRepo.existsByField(issueField)) {
            throw new BadRequestException(ISSUE_FIELD_IN_USE);
        }
    }

    private void ensureOptionNotInUse(FieldOption option) {
        if (optionRepo.isInUse(option)) {
            throw new BadRequestException(ISSUE_FIELD_OPTION_IN_USE);
        }
    }
}
