package com.tissue.issue.domain.exception;

import static com.tissue.issue.domain.exception.IssueErrorCode.*;

import java.time.Instant;
import java.util.List;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.common.exception.base.ResourceNotFoundException;
import com.tissue.issue.domain.enums.IssueHierarchy;
import com.tissue.issue.domain.enums.IssueRelationType;
import com.tissue.issuetype.domain.enums.FieldType;
import com.tissue.workflow.domain.enums.StateCategory;

public class IssueExceptions {

	private IssueExceptions() {
	}

	public static ResourceNotFoundException notFound(String workspaceKey, String issueKey) {
		return new ResourceNotFoundException(ISSUE_NOT_FOUND)
			.addContext("workspaceKey", workspaceKey)
			.addContext("issueKey", issueKey);
	}

	public static ResourceNotFoundException notFound(Long issueId) {
		return new ResourceNotFoundException(ISSUE_NOT_FOUND)
			.addContext("issueId", issueId);
	}

	public static BadRequestException invalidParentHierarchy(String workspaceKey, String parentIssueKey,
		IssueHierarchy parentHierarchy, String childIssueKey, IssueHierarchy childHierarchy) {
		return new BadRequestException(INVALID_PARENT_HIERARCHY)
			.addContext("workspaceKey", workspaceKey)
			.addContext("parentIssueKey", parentIssueKey)
			.addContext("parentHierarchy", parentHierarchy)
			.addContext("childIssueKey", childIssueKey)
			.addContext("childHierarchy", childHierarchy);
	}

	public static BadRequestException storyPointNotAllowed(String workspaceKey, String issueKey,
		IssueHierarchy currentHierarchy) {
		return new BadRequestException(STORY_POINT_NOT_ALLOWED)
			.addContext("workspaceKey", workspaceKey)
			.addContext("issueKey", issueKey)
			.addContext("currentHierarchy", currentHierarchy)
			.addContext("storyPointAllowedHierarchies", IssueHierarchy.getStoryPointModifiable());
	}

	public static BadRequestException relationCycleDetected(String sourceIssueKey, String targetIssueKey,
		IssueRelationType relationType, List<String> path) {
		return new BadRequestException(RELATION_CIRCULAR_DEPENDENCY)
			.addContext("sourceIssueKey", sourceIssueKey)
			.addContext("targetIssueKey", targetIssueKey)
			.addContext("relationType", relationType.name())
			.addContext("detectedCyclePath", path);
	}

	public static BadRequestException parentRequired(String workspaceKey, String issueKey,
		IssueHierarchy currentHierarchy) {
		return new BadRequestException(PARENT_REQUIRED)
			.addContext("workspaceKey", workspaceKey)
			.addContext("issueKey", issueKey)
			.addContext("currentHierarchy", currentHierarchy)
			.addContext("hierarchiesRequiringParent", IssueHierarchy.getParentRequired());
	}

	public static BadRequestException parentWorkspaceMismatch(String parentWorkspaceKey, String parentIssueKey,
		String childWorkspaceKey, String childIssueKey) {
		return new BadRequestException(PARENT_WORKSPACE_MISMATCH)
			.addContext("parentWorkspaceKey", parentWorkspaceKey)
			.addContext("parentIssueKey", parentIssueKey)
			.addContext("childWorkspaceKey", childWorkspaceKey)
			.addContext("childIssueKey", childIssueKey);
	}

	public static BadRequestException parentProjectMismatch(IssueHierarchy parentHierarchy, String parentIssueKey,
		IssueHierarchy childHierarchy, String childIssueKey) {
		return new BadRequestException(PARENT_PROJECT_MISMATCH)
			.addContext("parentHierarchy", parentHierarchy)
			.addContext("parentIssueKey", parentIssueKey)
			.addContext("childHierarchy", childHierarchy)
			.addContext("childIssueKey", childIssueKey);
	}

	public static BadRequestException transitionSourceStateMismatch(String workspaceKey, String issueKey,
		Long transitionId, String currentState, String requiredState) {
		return new BadRequestException(TRANSITION_SOURCE_STATE_NOT_MATCH)
			.addContext("workspaceKey", workspaceKey)
			.addContext("issueKey", issueKey)
			.addContext("transitionId", transitionId)
			.addContext("currentState", currentState)
			.addContext("requiredState", requiredState);
	}

	public static BadRequestException issueSelfReference(String workspaceKey, String issueKey) {
		return new BadRequestException(ISSUE_SELF_REFERENCE)
			.addContext("workspaceKey", workspaceKey)
			.addContext("issueKey", issueKey);
	}

	public static BadRequestException relationIssueTypeMismatch(String workspaceKey, IssueRelationType relationType,
		String sourceIssueKey, String sourceIssueType, String targetIssueKey, String targetIssueType) {
		return new BadRequestException(RELATION_ISSUE_TYPE_MISMATCH)
			.addContext("workspaceKey", workspaceKey)
			.addContext("relationType", relationType)
			.addContext("sourceIssueKey", sourceIssueKey)
			.addContext("sourceIssueType", sourceIssueType)
			.addContext("targetIssueKey", targetIssueKey)
			.addContext("targetIssueType", targetIssueType);
	}

	public static BadRequestException relationAlreadyExists(String workspaceKey, String sourceIssueKey,
		String targetIssueKey) {
		return new BadRequestException(RELATION_ALREADY_EXISTS)
			.addContext("workspaceKey", workspaceKey)
			.addContext("sourceIssueKey", sourceIssueKey)
			.addContext("targetIssueKey", targetIssueKey);
	}

	public static BadRequestException relationWorkspaceMismatch(String sourceWorkspaceKey, String sourceIssueKey,
		String targetWorkspaceKey, String targetIssueKey) {
		return new BadRequestException(RELATION_WORKSPACE_MISMATCH)
			.addContext("sourceWorkspaceKey", sourceWorkspaceKey)
			.addContext("sourceIssueKey", sourceIssueKey)
			.addContext("targetWorkspaceKey", targetWorkspaceKey)
			.addContext("targetIssueKey", targetIssueKey);
	}

	public static BadRequestException onlyInitialStateDeletionAllowed(String workspaceKey, String issueKey,
		String currentState, StateCategory stateCategory) {
		return new BadRequestException(ONLY_INITIAL_STATE_DELETION_ALLOWED)
			.addContext("workspaceKey", workspaceKey)
			.addContext("issueKey", issueKey)
			.addContext("currentState", currentState)
			.addContext("stateCategory", stateCategory);
	}

	public static BadRequestException cannotDeleteIssueWithChildren(String issueKey) {
		return new BadRequestException(CANNOT_DELETE_ISSUE_WITH_CHILDREN)
			.addContext("issueKey", issueKey);
	}

	public static BadRequestException dueDateMustBeFuture(Instant inputDate) {
		return new BadRequestException(DUE_DATE_MUST_BE_FUTURE)
			.addContext("inputDate", inputDate);
	}

	public static BadRequestException customFieldRequired(Long issueTypeId, String issueTypeName, Long fieldId,
		String fieldName) {
		return new BadRequestException(CUSTOM_FIELD_REQUIRED)
			.addContext("issueTypeId", issueTypeId)
			.addContext("issueType", issueTypeName)
			.addContext("issueFieldId", fieldId)
			.addContext("issueField", fieldName);
	}

	public static BadRequestException unknownCustomFieldId(Long fieldId) {
		return new BadRequestException(UNKNOWN_CUSTOM_FIELD_ID)
			.addContext("issueFieldId", fieldId);
	}

	public static BadRequestException customFieldTypeMismatch(Long fieldId, String fieldName, FieldType expectedType,
		Object inputValue) {
		return new BadRequestException(CUSTOM_FIELD_TYPE_MISMATCH)
			.addContext("issueFieldId", fieldId)
			.addContext("issueField", fieldName)
			.addContext("expectedType", expectedType)
			.addContext("inputValue", inputValue);
	}

	public static BadRequestException decimalScaleExceeded(Long fieldId, int maxFractionDigits) {
		return new BadRequestException(DECIMAL_SCALE_EXCEEDED)
			.addContext("issueFieldId", fieldId)
			.addContext("maxFractionDigits", maxFractionDigits);
	}

	public static BadRequestException integerDigitsExceeded(Long fieldId, int maxIntegerDigits) {
		return new BadRequestException(INTEGER_DIGITS_EXCEEDED)
			.addContext("issueFieldId", fieldId)
			.addContext("maxIntegerDigits", maxIntegerDigits);
	}

	public static BadRequestException maxReviewersExceeded(int maxReviewers) {
		return new BadRequestException(MAX_REVIEWERS_EXCEEDED)
			.addContext("maxReviewers", maxReviewers);
	}

	public static BadRequestException unknownEnumOption(Long fieldId, Long optionId) {
		return new BadRequestException(UNKNOWN_ENUM_OPTION)
			.addContext("issueFieldId", fieldId)
			.addContext("optionId", optionId);
	}

	public static ResourceNotFoundException reviewerNotFound(Long memberId) {
		return new ResourceNotFoundException(REVIEWER_NOT_FOUND)
			.addContext("memberId", memberId);
	}
}
