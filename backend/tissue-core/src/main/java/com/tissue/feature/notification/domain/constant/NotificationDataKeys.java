package com.tissue.feature.notification.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NotificationDataKeys {

    public static final String WORKSPACE_KEY = "workspaceKey";
    public static final String PROJECT_KEY = "projectKey";
    public static final String ISSUE_KEY = "issueKey";
    public static final String SPRINT_TITLE = "sprintTitle";
    public static final String TARGET_NAME = "targetName";
    public static final String ACTOR_NAME = "actorName";
    public static final String JOINED_MEMBER_NAME = "joinedMemberName";
    public static final String REMOVED_REVIEWER_NAME = "removedReviewerName";
    public static final String CONTENT = "content";
    public static final String CHANGED_FIELDS = "changedFields";
    public static final String OLD_STATE = "oldState";
    public static final String NEW_STATE = "newState";
    public static final String STATUS = "status";
    public static final String STARTED_AT = "startedAt";
    public static final String ENDED_AT = "endedAt";
    public static final String ROLE = "role";
    public static final String OLD_ROLE = "oldRole";
    public static final String NEW_ROLE = "newRole";
}
