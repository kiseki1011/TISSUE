package com.tissue.issuetype.domain.exception;

import static com.tissue.global.exception.ContextKeys.*;
import static com.tissue.issuetype.domain.exception.IssueTypeErrorCode.*;

import com.tissue.common.vo.Name;
import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.global.exception.base.ResourceNotFoundException;
import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.Project;

public class IssueTypeExceptions {

    private IssueTypeExceptions() {}

    public static ResourceNotFoundException typeNotFound(Long issueTypeId, Project project) {
        return new ResourceNotFoundException(ISSUE_TYPE_NOT_FOUND)
                .addContext(ISSUE_TYPE_ID, issueTypeId)
                .addContext(PROJECT_KEY, project.getKey())
                .addContext(WORKSPACE_KEY, project.getWorkspaceKey());
    }

    public static ResourceNotFoundException fieldNotFound(Long issueFieldId, IssueType issueType) {
        return new ResourceNotFoundException(ISSUE_FIELD_NOT_FOUND)
                .addContext(ISSUE_FIELD_ID, issueFieldId)
                .addContext(ISSUE_TYPE_ID, issueType.getId());
    }

    public static ResourceNotFoundException optionNotFound(Long optionId, IssueField issueField) {
        return new ResourceNotFoundException(FIELD_OPTION_NOT_FOUND)
                .addContext(FIELD_OPTION_ID, optionId)
                .addContext(ISSUE_FIELD_ID, issueField.getId());
    }

    public static ResourceConflictException duplicateTypeName(Name name, Project project) {
        return new ResourceConflictException(DUPLICATE_ISSUE_TYPE_NAME)
                .addContext(ISSUE_TYPE, name.getNormalized())
                .addContext(PROJECT_KEY, project.getKey());
    }

    public static ResourceConflictException duplicateFieldName(Name name, IssueType issueType) {
        return new ResourceConflictException(DUPLICATE_ISSUE_FIELD_NAME)
                .addContext(ISSUE_FIELD, name.getNormalized())
                .addContext(ISSUE_TYPE, issueType.getDisplayName())
                .addContext(ISSUE_TYPE_ID, issueType.getId());
    }

    public static ResourceConflictException duplicateOptionName(Name name, IssueField issueField) {
        return new ResourceConflictException(DUPLICATE_FIELD_OPTION_NAME)
                .addContext(ISSUE_FIELD_OPTION, name)
                .addContext(ISSUE_FIELD_ID, issueField.getId());
    }

    public static BadRequestException systemTypeNotDeletable(IssueType issueType) {
        return new BadRequestException(SYSTEM_ISSUE_TYPE_NOT_DELETABLE)
                .addContext(ISSUE_TYPE_ID, issueType.getId());
    }

    public static BadRequestException typeInUse(IssueType issueType) {
        return new BadRequestException(ISSUE_TYPE_IN_USE)
                .addContext(ISSUE_TYPE_ID, issueType.getId());
    }

    public static BadRequestException fieldInUse(IssueField issueField) {
        return new BadRequestException(ISSUE_FIELD_IN_USE)
                .addContext(ISSUE_FIELD_ID, issueField.getId());
    }

    public static BadRequestException optionInUse(EnumFieldOption option) {
        return new BadRequestException(FIELD_OPTION_IN_USE)
                .addContext(FIELD_OPTION_ID, option.getId());
    }

    public static BadRequestException optionLimitExceeded(int max, int current) {
        return new BadRequestException(OPTION_LIMIT_EXCEEDED)
                .addContext("maxOptions", max)
                .addContext("currentOptions", current);
    }

    public static BadRequestException optionReorderSizeMismatch(int expected, int actual) {
        return new BadRequestException(OPTION_REORDER_SIZE_MISMATCH)
                .addContext("expectedSize", expected)
                .addContext("actualSize", actual);
    }

    public static BadRequestException optionReorderDuplicateId() {
        return new BadRequestException(OPTION_REORDER_DUPLICATE_ID);
    }

    public static BadRequestException optionReorderUnknownId(Long unknownId) {
        return new BadRequestException(OPTION_REORDER_UNKNOWN_ID)
                .addContext("unknownOptionId", unknownId);
    }

    public static BadRequestException unsupportedFieldType(String fieldType, Object rawValue) {
        return new BadRequestException(UNSUPPORTED_FIELD_TYPE)
                .addContext("fieldType", fieldType)
                .addContext("rawValue", rawValue);
    }
}
