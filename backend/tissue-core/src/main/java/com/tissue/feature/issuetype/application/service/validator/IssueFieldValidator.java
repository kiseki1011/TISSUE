package com.tissue.feature.issuetype.application.service.validator;

import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.DUPLICATE_ISSUE_FIELD_NAME;
import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.ISSUE_FIELD_IN_USE;
import static com.tissue.feature.issuetype.domain.exception.IssueTypeErrorCode.ISSUE_FIELD_OPTION_IN_USE;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
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

    private final IssueFieldRepository issueFieldRepository;
    private final FieldOptionRepository optionRepository;
    private final IssueQueryRepository issueQueryRepository;

    public void ensureUniqueLabel(IssueType issueType, Name name) {
        boolean duplicated = issueFieldRepository.existsByIssueTypeAndName_Normalized(issueType, name.getNormalized());
        if (duplicated) {
            throw new ResourceConflictException(DUPLICATE_ISSUE_FIELD_NAME);
        }
    }

    public void ensureDeletable(IssueField issueField) {
        ensureFieldNotInUse(issueField);
    }

    public void ensureUniqueOptionLabel(IssueField field, Name name) {
        if (optionRepository.existsByIssueFieldAndName_Normalized(field, name.getNormalized())) {
            throw new ResourceConflictException(IssueTypeErrorCode.DUPLICATE_FIELD_OPTION_NAME);
        }
    }

    public void ensureOptionDeletable(FieldOption option) {
        ensureOptionNotInUse(option);
    }

    private void ensureFieldNotInUse(IssueField issueField) {
        if (issueQueryRepository.existsWithCustomField(String.valueOf(issueField.getId()))) {
            throw new BadRequestException(ISSUE_FIELD_IN_USE);
        }
    }

    private void ensureOptionNotInUse(FieldOption option) {
        String fieldIdStr = String.valueOf(option.getIssueField().getId());
        String optionIdStr = String.valueOf(option.getId());
        if (issueQueryRepository.isOptionInUse(fieldIdStr, optionIdStr)) {
            throw new BadRequestException(ISSUE_FIELD_OPTION_IN_USE);
        }
    }
}
