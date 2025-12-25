package com.tissue.security.authorization.project.issue;

public interface IssueSecurityExpressions {

	String REQUIRES_ISSUE_EDIT_PERMISSION = "@issueSecurityGuard.canEdit(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal)";

	String REQUIRES_ISSUE_DELETE_PERMISSION = "@issueSecurityGuard.canDelete(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal)";

	String REQUIRES_ISSUE_PARTICIPANT_MANAGE_PERMISSION = "@issueSecurityGuard.canManageParticipants(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal)";

	String REQUIRES_ISSUE_REVIEWER_MANAGE_PERMISSION = "@issueSecurityGuard.canManageReviewers(#cmd.workspaceKey, #cmd.projectKey, #cmd.issueKey, principal)";
}
