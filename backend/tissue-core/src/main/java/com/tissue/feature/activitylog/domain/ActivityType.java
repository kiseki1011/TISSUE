package com.tissue.feature.activitylog.domain;

import java.util.Arrays;
import java.util.List;

public enum ActivityType {
    ISSUE_CREATED,
    ISSUE_UPDATED,
    ISSUE_DELETED,
    ISSUE_RESTORED,
    ISSUE_ASSIGNED,
    ISSUE_UNASSIGNED,
    ISSUE_WORKFLOW_TRANSITIONED,
    ISSUE_STORY_POINT_CHANGED,
    ISSUE_PARENT_CHANGED,
    ISSUE_RELATION_ADDED,
    ISSUE_RELATION_REMOVED,
    ISSUE_REVIEW_REQUESTED,
    ISSUE_REVIEW_SUBMITTED,
    ISSUE_REVIEWER_ADDED,
    ISSUE_REVIEWER_REMOVED,
    ISSUE_COMMENT_ADDED,
    ISSUE_COMMENT_UPDATED,
    ISSUE_COMMENT_DELETED,
    ISSUE_BRANCH_CONNECTED,
    ISSUE_VCS_CONNECTION_LINKED,
    ISSUE_WORKFLOW_TRANSITIONED_BY_SYSTEM,
    SPRINT_STARTED,
    SPRINT_COMPLETED;

    /**
     * Issue-scoped activity types, comments included. Excludes sprint events.
     */
    public static List<ActivityType> issueTypes() {
        return Arrays.stream(values())
                .filter(type -> type.name().startsWith("ISSUE"))
                .toList();
    }
}
