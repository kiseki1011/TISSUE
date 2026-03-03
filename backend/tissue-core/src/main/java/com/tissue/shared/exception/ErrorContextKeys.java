package com.tissue.shared.exception;

public interface ErrorContextKeys {

    /**
     * Workspace
     */
    String WORKSPACE_KEY = "workspaceKey";

    /**
     * Invitation
     */
    String INVITATION_ID = "invitationId";

    /**
     * Link
     */
    String TOKEN = "token";

    /**
     * WorkspaceMember
     */
    String POSITION_ID = "positionId";

    String POSITION_NAME = "positionName";
    String TEAM_ID = "teamId";
    String TEAM_NAME = "teamName";
    String MAX_WORKSPACE_MEMBER = "maxWorkspaceMember";

    /**
     * Project
     */
    String PROJECT_KEY = "projectKey";

    /**
     * Issue
     */
    String ISSUE_KEY = "issueKey";

    String CHILD_ISSUE_KEY = "childIssueKey";
    String PARENT_ISSUE_KEY = "parentIssueKey";
    String SOURCE_ISSUE_KEY = "sourceIssueKey";
    String TARGET_ISSUE_KEY = "targetIssueKey";
    String CHILD_HIERARCHY = "childHierarchy";
    String PARENT_HIERARCHY = "parentHierarchy";
    String HIERARCHIES_REQUIRING_PARENT = "hierarchiesRequiringParent";
    String STORY_POINT_ALLOWED_HIERARCHIES = "storyPointAllowedHierarchies";
    String RELATION_TYPE = "relationType";

    /**
     * IssueType
     */
    String ISSUE_TYPE_ID = "issueTypeId";

    String ISSUE_TYPE_NAME = "issueTypeName";
    String SOURCE_ISSUE_TYPE = "sourceIssueTypeName";
    String TARGET_ISSUE_TYPE = "targetIssueTypeName";

    /**
     * IssueField
     */
    String ISSUE_FIELD_ID = "issueFieldId";

    String ISSUE_FIELD_NAME = "issueFieldName";
    String ISSUE_FIELD_OPTION_NAME = "fieldOptionName";
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
    String GUARD_TYPE = "guardType";

    /**
     * Sprint
     */
    String SPRINT_ID = "sprintId";

    String SPRINT_TITLE = "sprintTitle";

    /**
     * Comment
     */
    String COMMENT_ID = "commentId";

    String PARENT_COMMENT_ID = "parentCommentId";

    /**
     * Member
     */
    String MEMBER_ID = "memberId";

    String ACTOR_MEMBER_ID = "actorMemberId";
    String EMAIL = "email";
    String USERNAME = "username";
    String MEMBER_STATUS = "memberStatus";

    /**
     * NOTIFICATION
     */
    String NOTIFICATION_ID = "notificationId";

    /**
     * VCS
     */
    String VCS_PROVIDER = "vcsProvider";

    /**
     * ETC
     */
    String PROVIDED_VALUE = "providedValue";

    String EXPECTED_VALUE = "expectedValue";
    String CREATED_AT = "createdAt";
    String UPDATED_AT = "updatedAt";
    String DELETED_AT = "deletedAt";
    String REASON = "reason";
    String STATUS = "status";
}
