package com.tissue.shared.exception;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ErrorContextKeys {

    /**
     * Workspace
     */
    public static final String WORKSPACE_KEY = "workspaceKey";

    /**
     * Invitation
     */
    public static final String INVITATION_ID = "invitationId";

    /**
     * Link
     */
    public static final String TOKEN = "token";

    /**
     * WorkspaceMember
     */
    public static final String POSITION_ID = "positionId";

    public static final String POSITION_NAME = "positionName";
    public static final String TEAM_ID = "teamId";
    public static final String TEAM_NAME = "teamName";
    public static final String MAX_WORKSPACE_MEMBER = "maxWorkspaceMember";
    public static final String MAX_WORKSPACE_PROJECT = "maxWorkspaceProject";

    /**
     * Project
     */
    public static final String PROJECT_KEY = "projectKey";

    /**
     * Issue
     */
    public static final String ISSUE_KEY = "issueKey";

    public static final String CHILD_ISSUE_KEY = "childIssueKey";
    public static final String PARENT_ISSUE_KEY = "parentIssueKey";
    public static final String SOURCE_ISSUE_KEY = "sourceIssueKey";
    public static final String TARGET_ISSUE_KEY = "targetIssueKey";
    public static final String CHILD_HIERARCHY = "childHierarchy";
    public static final String PARENT_HIERARCHY = "parentHierarchy";
    public static final String HIERARCHIES_REQUIRING_PARENT = "hierarchiesRequiringParent";
    public static final String STORY_POINT_ALLOWED_HIERARCHIES = "storyPointAllowedHierarchies";
    public static final String RELATION_TYPE = "relationType";

    /**
     * Tag
     */
    public static final String TAG_ID = "tagId";

    /**
     * IssueType
     */
    public static final String ISSUE_TYPE_ID = "issueTypeId";

    public static final String ISSUE_TYPE_NAME = "issueTypeName";
    public static final String SOURCE_ISSUE_TYPE = "sourceIssueTypeName";
    public static final String TARGET_ISSUE_TYPE = "targetIssueTypeName";

    /**
     * IssueField
     */
    public static final String ISSUE_FIELD_ID = "issueFieldId";

    public static final String ISSUE_FIELD_NAME = "issueFieldName";
    public static final String ISSUE_FIELD_OPTION_NAME = "fieldOptionName";
    public static final String FIELD_OPTION_ID = "fieldOptionId";
    public static final String EXPECTED_TYPE = "expectedType";
    public static final String INPUT_VALUE = "inputValue";

    /**
     * Workflow
     */
    public static final String WORKFLOW_ID = "workflowId";

    public static final String STATE_ID = "stateId";
    public static final String TRANSITION_ID = "transitionId";
    public static final String WORKFLOW = "workflowName";
    public static final String STATE = "stateName";
    public static final String TRANSITION = "transitionName";
    public static final String CURRENT_STATE = "currentStateName";
    public static final String REQUIRED_STATE = "requiredStateName";
    public static final String GUARD_TYPE = "guardType";

    /**
     * Sprint
     */
    public static final String SPRINT_ID = "sprintId";

    public static final String SPRINT_TITLE = "sprintTitle";

    /**
     * Attachment
     */
    public static final String ATTACHMENT_ID = "attachmentId";

    /**
     * Comment
     */
    public static final String COMMENT_ID = "commentId";

    public static final String PARENT_COMMENT_ID = "parentCommentId";

    /**
     * Member
     */
    public static final String MEMBER_ID = "memberId";

    public static final String ACTOR_MEMBER_ID = "actorMemberId";
    public static final String EMAIL = "email";
    public static final String USERNAME = "username";
    public static final String MEMBER_STATUS = "memberStatus";

    /**
     * NOTIFICATION
     */
    public static final String NOTIFICATION_ID = "notificationId";

    /**
     * VCS
     */
    public static final String VCS_PROVIDER = "vcsProvider";

    /**
     * Wiki
     */
    public static final String WIKI_DOCUMENT_ID = "wikiDocumentId";

    public static final String WIKI_LINK_ID = "wikiLinkId";
    public static final String WIKI_LINK_TARGET_TYPE = "wikiLinkTargetType";
    public static final String WIKI_LINK_TARGET_ID = "wikiLinkTargetId";
    public static final String WIKI_ATTACHMENT_ID = "wikiAttachmentId";
    public static final String WIKI_SNAPSHOT_ID = "wikiSnapshotId";

    /**
     * Search / sort
     */
    public static final String SORT_PROPERTY = "sortProperty";

    public static final String ALLOWED_SORT_PROPERTIES = "allowedSortProperties";

    /**
     * ETC
     */
    public static final String PROVIDED_VALUE = "providedValue";

    public static final String EXPECTED_VALUE = "expectedValue";
    public static final String CREATED_AT = "createdAt";
    public static final String UPDATED_AT = "updatedAt";
    public static final String DELETED_AT = "deletedAt";
    public static final String REASON = "reason";
    public static final String STATUS = "status";
}
