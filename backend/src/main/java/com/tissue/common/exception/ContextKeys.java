package com.tissue.common.exception;

public interface ContextKeys {

	/**
	 * Workspace
	 */
	String WORKSPACE_KEY = "workspaceKey";
	String CHILD_WORKSPACE_KEY = "childWorkspaceKey";
	String PARENT_WORKSPACE_KEY = "parentWorkspaceKey";
	String SOURCE_WORKSPACE_KEY = "sourceWorkspaceKey";
	String TARGET_WORKSPACE_KEY = "targetWorkspaceKey";

	/**
	 * Project
	 */
	String PROJECT_KEY = "projectKey";
	String CHILD_PROJECT_KEY = "childProjectKey";
	String PARENT_PROJECT_KEY = "parentProjectKey";
	String SOURCE_PROJECT_KEY = "sourceProjectKey";
	String TARGET_PROJECT_KEY = "targetProjectKey";

	/**
	 * Issue
	 */
	String ISSUE_ID = "issueId";
	String ISSUE_KEY = "issueKey";
	String ISSUE_RELATION_ID = "issueRelationId";
	String CHILD_ISSUE_KEY = "childIssueKey";
	String PARENT_ISSUE_KEY = "parentIssueKey";
	String SOURCE_ISSUE_KEY = "sourceIssueKey";
	String TARGET_ISSUE_KEY = "targetIssueKey";
	String CHILD_HIERARCHY = "childHierarchy";
	String PARENT_HIERARCHY = "parentHierarchy";
	String CURRENT_HIERARCHY = "currentHierarchy";
	String HIERARCHIES_REQUIRING_PARENT = "hierarchiesRequiringParent";
	String STORY_POINT_ALLOWED_HIERARCHIES = "storyPointAllowedHierarchies";
	String RELATION_TYPE = "relationType";
	String INPUT_DATE = "inputDate";

	/**
	 * IssueType
	 */
	String ISSUE_TYPE_ID = "issueTypeId";
	String ISSUE_TYPE = "issueTypeName";
	String SOURCE_ISSUE_TYPE = "sourceIssueTypeName";
	String TARGET_ISSUE_TYPE = "targetIssueTypeName";

	/**
	 * IssueField
	 */
	String ISSUE_FIELD_ID = "issueFieldId";
	String ISSUE_FIELD = "issueField";
	String FIELD_OPTION = "fieldOption";
	String FIELD_OPTION_ID = "fieldOptionId";
	String EXPECTED_TYPE = "expectedType";
	String INPUT_VALUE = "inputValue";

	/**
	 * Workflow
	 */
	String WORKFLOW_ID = "workflowId";
	String STATE_ID = "stateId";
	String TRANSITION_ID = "transitionId";
	String WORKFLOW = "workflowName";
	String STATE = "stateName";
	String TRANSITION = "transitionName";
	String CURRENT_STATE = "currentStateName";
	String REQUIRED_STATE = "requiredStateName";
	String STATE_CATEGORY = "stateCategory";
	String GUARD_TYPE = "guardType";

	/**
	 * Sprint
	 */
	String SPRINT_ID = "sprintId";

	/**
	 * Member
	 */
	String MEMBER_ID = "memberId";

	/**
	 * ETC
	 */
	String PROVIDED_VALUE = "providedValue";
	String EXPECTED_VALUE = "expectedValue";
	String CREATED_AT = "createdAt";
	String UPDATED_AT = "updatedAt";
	String DELETED_AT = "deletedAt";
	String REASON = "reason";
}
