package com.tissue.activitylog.domain.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ActivityLogDataKeys {

    public static final String PROJECT_KEY = "projectKey";
    public static final String ISSUE_KEY = "issueKey";
    public static final String ACTOR_NAME = "actorName";
    public static final String ACTOR_EMAIL = "actorEmail";

    public static final String OLD_STATE = "oldState";
    public static final String NEW_STATE = "newState";
    public static final String STATE = "state"; // field name for change

    public static final String ASSIGNEE_NAME = "assigneeName";
    public static final String REMOVED_ASSIGNEE_NAME = "removedAssigneeName";

    public static final String OLD_REPORTER = "oldReporter";
    public static final String NEW_REPORTER = "newReporter";
    public static final String REPORTER = "reporter"; // field name for change

    public static final String REVIEWER_NAME = "reviewerName";
    public static final String REMOVED_REVIEWER_NAME = "removedReviewerName";
    public static final String REVIEW_STATUS = "reviewStatus";
    public static final String REVIEWER_COUNT = "reviewerCount";

    public static final String OLD_POINT = "oldPoint";
    public static final String NEW_POINT = "newPoint";
    public static final String STORY_POINT = "storyPoint"; // field name for change

    public static final String OLD_PARENT = "oldParent";
    public static final String NEW_PARENT = "newParent";
    public static final String PARENT = "parent"; // field name for change

    public static final String SOURCE_ISSUE_KEY = "sourceIssueKey";
    public static final String RELATION_TYPE = "relationType";
    public static final String TARGET_ISSUE_KEY = "targetIssueKey";

    public static final String SPRINT_TITLE = "sprintTitle";

    public static final String VCS_PROVIDER = "vcsProvider";

    public static final String BRANCH_NAME = "branchName";
    public static final String REPO_URL = "repoUrl";

    public static final String PR_TITLE = "prTitle";
    public static final String PR_URL = "prUrl";
    public static final String PR_ACTION = "prAction";
    public static final String VCS_USER_EMAIL = "vcsUserEmail";
    public static final String VCS_USER_NAME = "vcsUserName";
    public static final String TRIGGER_REASON = "triggerReason";
}
