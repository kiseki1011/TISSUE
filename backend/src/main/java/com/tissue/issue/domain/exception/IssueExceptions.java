package com.tissue.issue.domain.exception;

import static com.tissue.global.exception.ContextKeys.CHILD_HIERARCHY;
import static com.tissue.global.exception.ContextKeys.CHILD_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.CHILD_WORKSPACE_KEY;
import static com.tissue.global.exception.ContextKeys.CURRENT_HIERARCHY;
import static com.tissue.global.exception.ContextKeys.CURRENT_STATE;
import static com.tissue.global.exception.ContextKeys.EXPECTED_TYPE;
import static com.tissue.global.exception.ContextKeys.FIELD_OPTION_ID;
import static com.tissue.global.exception.ContextKeys.HIERARCHIES_REQUIRING_PARENT;
import static com.tissue.global.exception.ContextKeys.INPUT_DATE;
import static com.tissue.global.exception.ContextKeys.INPUT_VALUE;
import static com.tissue.global.exception.ContextKeys.ISSUE_FIELD;
import static com.tissue.global.exception.ContextKeys.ISSUE_FIELD_ID;
import static com.tissue.global.exception.ContextKeys.ISSUE_ID;
import static com.tissue.global.exception.ContextKeys.ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.ISSUE_TYPE;
import static com.tissue.global.exception.ContextKeys.ISSUE_TYPE_ID;
import static com.tissue.global.exception.ContextKeys.MEMBER_ID;
import static com.tissue.global.exception.ContextKeys.PARENT_HIERARCHY;
import static com.tissue.global.exception.ContextKeys.PARENT_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.PARENT_WORKSPACE_KEY;
import static com.tissue.global.exception.ContextKeys.PROVIDED_VALUE;
import static com.tissue.global.exception.ContextKeys.RELATION_TYPE;
import static com.tissue.global.exception.ContextKeys.REQUIRED_STATE;
import static com.tissue.global.exception.ContextKeys.SOURCE_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.SOURCE_ISSUE_TYPE;
import static com.tissue.global.exception.ContextKeys.SOURCE_WORKSPACE_KEY;
import static com.tissue.global.exception.ContextKeys.STATE_CATEGORY;
import static com.tissue.global.exception.ContextKeys.STORY_POINT_ALLOWED_HIERARCHIES;
import static com.tissue.global.exception.ContextKeys.TARGET_ISSUE_KEY;
import static com.tissue.global.exception.ContextKeys.TARGET_ISSUE_TYPE;
import static com.tissue.global.exception.ContextKeys.TARGET_WORKSPACE_KEY;
import static com.tissue.global.exception.ContextKeys.TRANSITION_ID;
import static com.tissue.global.exception.ContextKeys.WORKSPACE_KEY;
import static com.tissue.issue.domain.exception.IssueErrorCode.CANNOT_DELETE_ISSUE_WITH_CHILDREN;
import static com.tissue.issue.domain.exception.IssueErrorCode.CUSTOM_FIELD_REQUIRED;
import static com.tissue.issue.domain.exception.IssueErrorCode.CUSTOM_FIELD_TYPE_MISMATCH;
import static com.tissue.issue.domain.exception.IssueErrorCode.DECIMAL_SCALE_EXCEEDED;
import static com.tissue.issue.domain.exception.IssueErrorCode.DUE_DATE_MUST_BE_FUTURE;
import static com.tissue.issue.domain.exception.IssueErrorCode.INTEGER_DIGITS_EXCEEDED;
import static com.tissue.issue.domain.exception.IssueErrorCode.INVALID_PARENT_HIERARCHY;
import static com.tissue.issue.domain.exception.IssueErrorCode.INVALID_PERCENTAGE_EXCEPTION;
import static com.tissue.issue.domain.exception.IssueErrorCode.ISSUE_NOT_FOUND;
import static com.tissue.issue.domain.exception.IssueErrorCode.ISSUE_SELF_REFERENCE;
import static com.tissue.issue.domain.exception.IssueErrorCode.MAX_REVIEWERS_EXCEEDED;
import static com.tissue.issue.domain.exception.IssueErrorCode.ONLY_INITIAL_STATE_DELETION_ALLOWED;
import static com.tissue.issue.domain.exception.IssueErrorCode.PARENT_PROJECT_MISMATCH;
import static com.tissue.issue.domain.exception.IssueErrorCode.PARENT_REQUIRED;
import static com.tissue.issue.domain.exception.IssueErrorCode.PARENT_WORKSPACE_MISMATCH;
import static com.tissue.issue.domain.exception.IssueErrorCode.RELATION_ALREADY_EXISTS;
import static com.tissue.issue.domain.exception.IssueErrorCode.RELATION_CIRCULAR_DEPENDENCY;
import static com.tissue.issue.domain.exception.IssueErrorCode.RELATION_ISSUE_TYPE_MISMATCH;
import static com.tissue.issue.domain.exception.IssueErrorCode.RELATION_NOT_FOUND;
import static com.tissue.issue.domain.exception.IssueErrorCode.RELATION_WORKSPACE_MISMATCH;
import static com.tissue.issue.domain.exception.IssueErrorCode.REVIEWER_NOT_FOUND;
import static com.tissue.issue.domain.exception.IssueErrorCode.STORY_POINT_NOT_ALLOWED;
import static com.tissue.issue.domain.exception.IssueErrorCode.TRANSITION_SOURCE_STATE_NOT_MATCH;
import static com.tissue.issue.domain.exception.IssueErrorCode.UNKNOWN_CUSTOM_FIELD_ID;
import static com.tissue.issue.domain.exception.IssueErrorCode.UNKNOWN_ENUM_OPTION;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ResourceNotFoundException;
import com.tissue.issue.domain.enums.IssueHierarchy;
import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.issuetype.domain.enums.IssueFieldType;
import com.tissue.workflow.domain.enums.StateCategory;
import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;

public class IssueExceptions {

    private IssueExceptions() {}

    public static ResourceNotFoundException notFound(String workspaceKey, String issueKey) {
        return new ResourceNotFoundException(ISSUE_NOT_FOUND)
                .addContext(WORKSPACE_KEY, workspaceKey)
                .addContext(ISSUE_KEY, issueKey);
    }

    public static ResourceNotFoundException notFound(Long issueId) {
        return new ResourceNotFoundException(ISSUE_NOT_FOUND).addContext(ISSUE_ID, issueId);
    }

    public static BadRequestException invalidParentHierarchy(
            String workspaceKey,
            String parentIssueKey,
            IssueHierarchy parentHierarchy,
            String childIssueKey,
            IssueHierarchy childHierarchy) {
        return new BadRequestException(INVALID_PARENT_HIERARCHY)
                .addContext(WORKSPACE_KEY, workspaceKey)
                .addContext(PARENT_ISSUE_KEY, parentIssueKey)
                .addContext(PARENT_HIERARCHY, parentHierarchy)
                .addContext(CHILD_ISSUE_KEY, childIssueKey)
                .addContext(CHILD_HIERARCHY, childHierarchy);
    }

    public static BadRequestException storyPointNotAllowed(
            String workspaceKey, String issueKey, IssueHierarchy currentHierarchy) {
        return new BadRequestException(STORY_POINT_NOT_ALLOWED)
                .addContext(WORKSPACE_KEY, workspaceKey)
                .addContext(ISSUE_KEY, issueKey)
                .addContext(CURRENT_HIERARCHY, currentHierarchy)
                .addContext(STORY_POINT_ALLOWED_HIERARCHIES, IssueHierarchy.getStoryPointModifiable());
    }

    public static BadRequestException relationCycleDetected(
            String sourceIssueKey, String targetIssueKey, IssueRelationType relationType, List<String> path) {
        return new BadRequestException(RELATION_CIRCULAR_DEPENDENCY)
                .addContext(SOURCE_ISSUE_KEY, sourceIssueKey)
                .addContext(TARGET_ISSUE_KEY, targetIssueKey)
                .addContext(RELATION_TYPE, relationType.name())
                .addContext("detectedCyclePath", path);
    }

    public static BadRequestException parentRequired(
            String workspaceKey, String issueKey, IssueHierarchy currentHierarchy) {
        return new BadRequestException(PARENT_REQUIRED)
                .addContext(WORKSPACE_KEY, workspaceKey)
                .addContext(ISSUE_KEY, issueKey)
                .addContext(CURRENT_HIERARCHY, currentHierarchy)
                .addContext(HIERARCHIES_REQUIRING_PARENT, IssueHierarchy.getParentRequired());
    }

    public static BadRequestException parentWorkspaceMismatch(
            String parentWorkspaceKey, String parentIssueKey, String childWorkspaceKey, String childIssueKey) {
        return new BadRequestException(PARENT_WORKSPACE_MISMATCH)
                .addContext(PARENT_WORKSPACE_KEY, parentWorkspaceKey)
                .addContext(PARENT_ISSUE_KEY, parentIssueKey)
                .addContext(CHILD_WORKSPACE_KEY, childWorkspaceKey)
                .addContext(CHILD_ISSUE_KEY, childIssueKey);
    }

    public static BadRequestException parentProjectMismatch(
            IssueHierarchy parentHierarchy,
            String parentIssueKey,
            IssueHierarchy childHierarchy,
            String childIssueKey) {
        return new BadRequestException(PARENT_PROJECT_MISMATCH)
                .addContext(PARENT_HIERARCHY, parentHierarchy)
                .addContext(PARENT_ISSUE_KEY, parentIssueKey)
                .addContext(CHILD_HIERARCHY, childHierarchy)
                .addContext(CHILD_ISSUE_KEY, childIssueKey);
    }

    public static BadRequestException transitionSourceStateMismatch(
            String workspaceKey, String issueKey, Long transitionId, String currentState, String requiredState) {
        return new BadRequestException(TRANSITION_SOURCE_STATE_NOT_MATCH)
                .addContext(WORKSPACE_KEY, workspaceKey)
                .addContext(ISSUE_KEY, issueKey)
                .addContext(TRANSITION_ID, transitionId)
                .addContext(CURRENT_STATE, currentState)
                .addContext(REQUIRED_STATE, requiredState);
    }

    public static BadRequestException issueSelfReference(String workspaceKey, String issueKey) {
        return new BadRequestException(ISSUE_SELF_REFERENCE)
                .addContext(WORKSPACE_KEY, workspaceKey)
                .addContext(ISSUE_KEY, issueKey);
    }

    public static BadRequestException relationIssueTypeMismatch(
            String workspaceKey,
            IssueRelationType relationType,
            String sourceIssueKey,
            String sourceIssueType,
            String targetIssueKey,
            String targetIssueType) {
        return new BadRequestException(RELATION_ISSUE_TYPE_MISMATCH)
                .addContext(WORKSPACE_KEY, workspaceKey)
                .addContext(RELATION_TYPE, relationType)
                .addContext(SOURCE_ISSUE_KEY, sourceIssueKey)
                .addContext(SOURCE_ISSUE_TYPE, sourceIssueType)
                .addContext(TARGET_ISSUE_KEY, targetIssueKey)
                .addContext(TARGET_ISSUE_TYPE, targetIssueType);
    }

    public static BadRequestException relationAlreadyExists(
            String workspaceKey, String sourceIssueKey, String targetIssueKey) {
        return new BadRequestException(RELATION_ALREADY_EXISTS)
                .addContext(WORKSPACE_KEY, workspaceKey)
                .addContext(SOURCE_ISSUE_KEY, sourceIssueKey)
                .addContext(TARGET_ISSUE_KEY, targetIssueKey);
    }

    public static ResourceNotFoundException relationNotFound(
            String workspaceKey, String sourceIssueKey, String targetIssueKey) {
        return new ResourceNotFoundException(RELATION_NOT_FOUND)
                .addContext(WORKSPACE_KEY, workspaceKey)
                .addContext(SOURCE_ISSUE_KEY, sourceIssueKey)
                .addContext(TARGET_ISSUE_KEY, targetIssueKey);
    }

    public static BadRequestException relationWorkspaceMismatch(
            String sourceWorkspaceKey, String sourceIssueKey, String targetWorkspaceKey, String targetIssueKey) {
        return new BadRequestException(RELATION_WORKSPACE_MISMATCH)
                .addContext(SOURCE_WORKSPACE_KEY, sourceWorkspaceKey)
                .addContext(SOURCE_ISSUE_KEY, sourceIssueKey)
                .addContext(TARGET_WORKSPACE_KEY, targetWorkspaceKey)
                .addContext(TARGET_ISSUE_KEY, targetIssueKey);
    }

    public static BadRequestException onlyInitialStateDeletionAllowed(
            String workspaceKey, String issueKey, String currentState, StateCategory stateCategory) {
        return new BadRequestException(ONLY_INITIAL_STATE_DELETION_ALLOWED)
                .addContext(WORKSPACE_KEY, workspaceKey)
                .addContext(ISSUE_KEY, issueKey)
                .addContext(CURRENT_STATE, currentState)
                .addContext(STATE_CATEGORY, stateCategory);
    }

    public static BadRequestException cannotDeleteIssueWithChildren(String issueKey) {
        return new BadRequestException(CANNOT_DELETE_ISSUE_WITH_CHILDREN).addContext(ISSUE_KEY, issueKey);
    }

    public static BadRequestException dueDateMustBeFuture(Instant inputDate) {
        return new BadRequestException(DUE_DATE_MUST_BE_FUTURE).addContext(INPUT_DATE, inputDate);
    }

    public static ResourceNotFoundException reviewerNotFound(Long memberId) {
        return new ResourceNotFoundException(REVIEWER_NOT_FOUND).addContext(MEMBER_ID, memberId);
    }

    public static BadRequestException customFieldRequired(
            Long issueTypeId, String issueTypeName, Long fieldId, String fieldName) {
        return new BadRequestException(CUSTOM_FIELD_REQUIRED)
                .addContext(ISSUE_TYPE_ID, issueTypeId)
                .addContext(ISSUE_TYPE, issueTypeName)
                .addContext(ISSUE_FIELD_ID, fieldId)
                .addContext(ISSUE_FIELD, fieldName);
    }

    public static BadRequestException unknownCustomFieldId(Long fieldId) {
        return new BadRequestException(UNKNOWN_CUSTOM_FIELD_ID).addContext(ISSUE_FIELD_ID, fieldId);
    }

    public static BadRequestException customFieldTypeMismatch(
            Long fieldId, String fieldName, IssueFieldType expectedType, @Nullable Object inputValue) {
        return new BadRequestException(CUSTOM_FIELD_TYPE_MISMATCH)
                .addContext(ISSUE_FIELD_ID, fieldId)
                .addContext(ISSUE_FIELD, fieldName)
                .addContext(EXPECTED_TYPE, expectedType)
                .addContext(INPUT_VALUE, inputValue);
    }

    public static BadRequestException decimalScaleExceeded(Long fieldId, int maxFractionDigits) {
        return new BadRequestException(DECIMAL_SCALE_EXCEEDED)
                .addContext(ISSUE_FIELD_ID, fieldId)
                .addContext("maxFractionDigits", maxFractionDigits);
    }

    public static BadRequestException integerDigitsExceeded(Long fieldId, int maxIntegerDigits) {
        return new BadRequestException(INTEGER_DIGITS_EXCEEDED)
                .addContext(ISSUE_FIELD_ID, fieldId)
                .addContext("maxIntegerDigits", maxIntegerDigits);
    }

    public static BadRequestException maxReviewersExceeded(int maxReviewers) {
        return new BadRequestException(MAX_REVIEWERS_EXCEEDED).addContext("maxReviewers", maxReviewers);
    }

    public static BadRequestException unknownEnumOption(Long fieldId, Long optionId) {
        return new BadRequestException(UNKNOWN_ENUM_OPTION)
                .addContext(ISSUE_FIELD_ID, fieldId)
                .addContext(FIELD_OPTION_ID, optionId);
    }

    public static BadRequestException invalidPercentage(Integer inputValue) {
        return new BadRequestException(INVALID_PERCENTAGE_EXCEPTION).addContext(PROVIDED_VALUE, inputValue);
    }
}
